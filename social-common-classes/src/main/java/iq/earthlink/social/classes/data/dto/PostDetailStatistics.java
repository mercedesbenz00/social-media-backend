package iq.earthlink.social.classes.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDetailStatistics {
    private long postId;
    private double score;
    private long commentsCount;
    private long upvotesCount;
    private long downvotesCount;
    private long voteValue = 0L;
}
