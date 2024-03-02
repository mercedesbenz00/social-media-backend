package iq.earthlink.social.userfeedaggregatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedAggregatorResponse {
    private String postUuid;
    private Long userGroupId;
}
