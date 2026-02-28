package info.search.repository;

import info.search.model.Doc;
import info.search.model.DocChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends JpaRepository<DocChunk, Long> {
    List<DocChunk> findByDoc(Doc doc);
}
