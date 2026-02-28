package info.search.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocShortDto {
    private Long id;
    private String fileName;
    private String title;
    private String author;
}
