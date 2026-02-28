package info.search.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDto {
    private String query;
    private int page;
    private int size;
    private Long docId;
}
