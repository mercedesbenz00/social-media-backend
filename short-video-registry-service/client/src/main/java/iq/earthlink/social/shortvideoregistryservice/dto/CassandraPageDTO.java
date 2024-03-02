package iq.earthlink.social.shortvideoregistryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CassandraPageDTO<T> {
    private Integer count;
    private List<T> content;
    private String pagingState;
    private Boolean hasNext;

    public CassandraPageDTO(final Slice<T> slice, String pagingState) {
        this.content = slice.getContent();
        this.count = content.size();
        this.hasNext = slice.hasNext();
        this.pagingState = pagingState;
    }
}
