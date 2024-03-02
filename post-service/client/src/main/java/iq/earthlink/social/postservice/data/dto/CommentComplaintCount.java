package iq.earthlink.social.postservice.data.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@ApiModel(description = "DTO for comment complaints info")
public class CommentComplaintCount {
    @ApiModelProperty("comment id")
    private long id;

    @ApiModelProperty("comment content")
    private String content;

    @ApiModelProperty("comment post id")
    private long postId;

    @ApiModelProperty("comment author id")
    private String authorId;

    @ApiModelProperty("user group id (from post)")
    private Long userGroupId;

    @ApiModelProperty("complaints count")
    private long complaintsCount;
}
