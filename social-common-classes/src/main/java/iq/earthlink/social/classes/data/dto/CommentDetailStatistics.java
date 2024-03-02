package iq.earthlink.social.classes.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDetailStatistics {
    private long commentId;
    private long replyCommentsCount;
    private long upvotesCount;
    private long downvotesCount;
    private long voteValue = 0L;
}
