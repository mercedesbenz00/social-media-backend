package iq.earthlink.social.postservice.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {
    private Long id;
    private String content;
    private String authorId;
    private int repliesCount;
    private Date createdAt;
    private Date modifiedAt;
    private boolean edited;
    private List<CommentDto> replies;
    private long replyCommentsCount;
}
