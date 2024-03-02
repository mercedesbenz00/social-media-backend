package iq.earthlink.social.userfeedaggregatorservice.dto;

import iq.earthlink.social.classes.data.dto.PostResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponse {
    private List<PostResponse> content;
    private long number;
    private long totalPages;
    private long totalElements;
    private long timestamp;
    private long size;
}
