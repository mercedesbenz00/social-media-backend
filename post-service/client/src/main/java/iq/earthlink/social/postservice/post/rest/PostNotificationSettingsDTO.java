package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "User Post Notification Settings DTO")
@FieldNameConstants
@Builder
public class PostNotificationSettingsDTO {
    @ApiModelProperty("Post id")
    private Long postId;

    @ApiModelProperty("Is the notification muted from this post")
    private boolean isMuted;
}
