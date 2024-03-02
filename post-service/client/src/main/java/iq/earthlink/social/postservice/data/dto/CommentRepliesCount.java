package iq.earthlink.social.postservice.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CommentRepliesCount {
    private Long commentId;
    private Long firstSubCommentId;
    private Long repliesCount;
}
