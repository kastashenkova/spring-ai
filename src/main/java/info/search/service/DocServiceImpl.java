package info.search.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import info.search.Fb2Parser;
import info.search.dto.DocDto;
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
    public Page<DocDto> getAll(Pageable pageable) {
        return repo.findAll(pageable)
                .map(docMapper::toDto);
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

    @Transactional
    @Override
    public DocDto uploadDocument(MultipartFile file) {
        try {
            Doc doc = Fb2Parser.parse(file.getInputStream());
            doc.setFileName(file.getOriginalFilename());
            DocDto saved = docMapper.toDto(repo.save(doc));

            Document document = new Document(
                    doc.getContent(),
                    Map.of("docId", saved.getId(), "title", doc.getTitle())
            );
            List<Document> chunks = splitter.apply(List.of(document));
            vectorStore.add(chunks);

            List<DocChunk> docChunks = chunks.stream()
                    .map(c -> {
                        DocChunk dc = new DocChunk();
                        dc.setVectorId(c.getId());
                        dc.setDoc(docMapper.toEntity(saved));
                        return dc;
                    }).toList();
            chunkRepo.saveAll(docChunks);
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

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(pageable.getPageSize());

        if (docId != null) {
            builder.filterExpression("docId == " + docId);
        }

        List<SearchResultDto> res = Objects.requireNonNull(
                vectorStore.similaritySearch(builder.build()))
                .stream()
                .map(doc -> {
                    Long foundDocId = ((Number) doc.getMetadata()
                            .get("docId"))
                            .longValue();
                    String title = (String) doc.getMetadata().get("title");
                    return new SearchResultDto(foundDocId, title, doc.getText());
                })
                .toList();

        return new PageImpl<>(res, pageable, res.size());
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
