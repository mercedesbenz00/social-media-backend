package iq.earthlink.social.userfeedaggregatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedRequest {
    private List<Long> groupIds;
    private Long timestamp;
    private int page;
    private Integer size = 10;
}
