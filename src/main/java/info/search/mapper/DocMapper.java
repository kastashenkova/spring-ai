package info.search.mapper;

import info.search.config.MapperConfig;
import info.search.dto.DocDto;
import info.search.dto.DocShortDto;
import info.search.model.Doc;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface DocMapper {
    DocDto toDto(Doc doc);

    DocShortDto toShortDto(Doc doc);

    Doc toEntity(DocDto dto);
}
