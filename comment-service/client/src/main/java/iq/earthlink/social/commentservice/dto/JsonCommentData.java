package iq.earthlink.social.commentservice.dto;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.validation.NewEntityGroup;
import lombok.*;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonCommentData implements CommentData {
    @ApiModelProperty("The text content of the comment")
    @NotBlank(groups = NewEntityGroup.class)
    private String content;

    @ApiModelProperty("Comment author ID")
    private Long authorId;

    @ApiModelProperty("Unique identifier of commented object (UUID of post, video, etc.)")
    private UUID objectId;

    @ApiModelProperty("IDs of persons mentioned in the comment")
    private List<Long> mentionedPersonIds;
}
