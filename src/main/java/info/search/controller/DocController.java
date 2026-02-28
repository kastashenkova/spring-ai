package info.search.controller;

import info.search.dto.DocDto;
import info.search.dto.SearchDto;
import info.search.dto.SearchResultDto;
import info.search.model.Doc;
import info.search.service.DocService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Documents management",
        description = "Endpoints for managing documents")
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/docs")
public class DocController {
    private final DocService service;

    @GetMapping()
    @Operation(summary = "Get all docs",
            description = "Get all documents existing in db")
    public Page<DocDto> getAllDocs(Pageable pageable) {
        return service.getAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Search doc by its id",
            description = "Get document's name by its id in database")
    public DocDto getById(@PathVariable long id) {
        return service.getById(id)
                .orElse(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete doc by its id",
            description = "Delete doc from database by its id")
    public ResponseEntity<Void> deleteById(@PathVariable long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload fb2 document",
            description = "Upload fb2 document into the database")
    public ResponseEntity<DocDto> uploadDocument(@RequestParam("file") MultipartFile file) {
        DocDto saved = service.uploadDocument(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/search")
    @Operation(summary = "Semantic search",
            description = "Search information in database documents")
    public Page<SearchResultDto> search(@RequestBody SearchDto dto) {
        return service.search(
                dto.getQuery(),
                dto.getDocId(),
                PageRequest.of(dto.getPage(),
                        dto.getSize())
        );
    }
}
