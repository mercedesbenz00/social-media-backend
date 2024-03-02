package iq.earthlink.social.personservice.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEventDTO {
    private Long personId;
    private Long groupId;
    private Long postId;
    private PostEventType eventType;
}
