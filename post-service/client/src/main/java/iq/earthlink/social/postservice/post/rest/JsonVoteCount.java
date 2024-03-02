package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonVoteCount {

    private Long id;

    @ApiModelProperty(value = "The total number of upvotes")
    private Long upvotesTotal;

    @ApiModelProperty(value = "The total number of downvotes")
    private Long downvotesTotal;

    @ApiModelProperty(value = "Logged-in user vote value: 1 (upvote), 2 (downvote), or 0 (n/a)")
    private int voteValue = 0;
}
