package iq.earthlink.social.shortvideoregistryservice.repository;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchCriteria {
    private Long authorId;
    private List<String> categories;
    private List<Long> tags;
}
