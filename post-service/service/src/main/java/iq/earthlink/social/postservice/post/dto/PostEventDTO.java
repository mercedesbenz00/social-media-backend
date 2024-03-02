package iq.earthlink.social.postservice.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEventDTO {
    private Long personId;
    private Long groupId;
    private Long postId;
    private String postUuid;
    private Date publishedAt;
    private PostEventType eventType;
}
