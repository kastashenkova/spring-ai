package info.search.service;

import java.util.Optional;

import info.search.dto.DocDto;
import info.search.dto.DocShortDto;
import info.search.dto.SearchResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface DocService {

    Page<DocShortDto> getAll(Pageable pageable);

    DocDto uploadDocument(MultipartFile file);

    Optional<DocDto> getById(Long id);

    Page<SearchResultDto> search(String query,
                                 Long docId,
                                 Pageable pageable);

    void deleteById(Long id);
}
