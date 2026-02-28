package info.search.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import info.search.Fb2Parser;
import info.search.dto.DocDto;
import info.search.dto.DocShortDto;
import info.search.dto.SearchResultDto;
import info.search.exception.EmptyQueryException;
import info.search.exception.UnknownDocumentException;
import info.search.exception.UploadingDocumentException;
import info.search.mapper.DocMapper;
import info.search.model.Doc;
import info.search.model.DocChunk;
import info.search.repository.ChunkRepository;
import info.search.repository.DocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocServiceImpl implements DocService {
    private final DocRepository repo;
    private final DocMapper docMapper;
    private final ChunkRepository chunkRepo;
    private final TokenTextSplitter splitter;
    private final VectorStore vectorStore;

    @Override
    public Page<DocShortDto> getAll(Pageable pageable) {
        return repo.findAll(pageable)
                .map(docMapper::toShortDto);
    }

    @Override
    public Optional<DocDto> getById(Long docId) {
        if (Objects.isNull(docId)) {
            return Optional.empty();
        }
        if (!repo.existsById(docId)) {
            throw new UnknownDocumentException(
                    "Document with id " + docId + " not found");
        }
        return repo.findById(docId)
                .map(docMapper::toDto);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocDto uploadDocument(MultipartFile file) {
        try {
            Doc doc = Fb2Parser.parse(file.getInputStream());
            doc.setFileName(file.getOriginalFilename());
            DocDto saved = docMapper.toDto(repo.save(doc));

            Document document = new Document(
                    doc.getContent(),
                    Map.of("docId", saved.getId(),
                            "title", doc.getTitle(),
                            "author", doc.getAuthor())
            );
            List<Document> chunks = splitter.apply(List.of(document));
            List<DocChunk> docChunks = chunks.stream()
                    .map(c -> {
                        DocChunk dc = new DocChunk();
                        dc.setVectorId(c.getId());
                        dc.setDoc(docMapper.toEntity(saved));
                        return dc;
                    }).toList();
            chunkRepo.saveAll(docChunks);
            try {
                vectorStore.add(chunks);
            } catch (Exception e) {
                throw new UploadingDocumentException("Vector store failed: " + file, e);
            }
            return saved;
        } catch (UploadingDocumentException e) {
            throw e;
        } catch (Exception e) {
            throw new UploadingDocumentException(
                    "Error while uploading document: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public Page<SearchResultDto> search(String query, Long docId, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new EmptyQueryException("Query string is empty");
        }
        if (docId != null && !repo.existsById(docId)) {
            throw new UnknownDocumentException("Document with id " + docId + " not found");
        }

        int topK = (pageable.getPageNumber() + 1) * pageable.getPageSize() + 1;

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.002);

        if (docId != null) {
            builder.filterExpression("docId == " + docId);
        }

        List<SearchResultDto> list = Objects.requireNonNull(
                        vectorStore.similaritySearch(builder.build()))
                .stream()
                .map(doc -> {
                    Long foundDocId = ((Number) doc.getMetadata().get("docId")).longValue();
                    String title = (String) doc.getMetadata().get("title");
                    String author = (String) doc.getMetadata().get("author");
                    return new SearchResultDto(foundDocId, title, author, doc.getText());
                })
                .toList();

        boolean hasNext = list.size() > (pageable.getPageNumber() + 1) * pageable.getPageSize();

        List<SearchResultDto> res = list.subList(
                pageable.getPageNumber() * pageable.getPageSize(),
                Math.min(list.size(), (pageable.getPageNumber() + 1) * pageable.getPageSize())
        );

        long total = hasNext
                ? (long) (pageable.getPageNumber() + 2) * pageable.getPageSize()
                : (long) pageable.getPageNumber() * pageable.getPageSize() + res.size();

        return new PageImpl<>(res, pageable, total);
    }

    @Transactional
    @Override
    public void deleteById(Long docId) {
        Doc doc = repo.findById(docId).orElseThrow(() -> new UnknownDocumentException(
                "Document with id " + docId + " not found")
        );
        List<DocChunk> chunks = chunkRepo.findByDoc(doc);
        List<String> vectorIds = chunks.stream()
                .map(DocChunk::getVectorId)
                .toList();

        vectorStore.delete(vectorIds);
        chunkRepo.deleteAll(chunks);
        repo.delete(doc);
    }
}
