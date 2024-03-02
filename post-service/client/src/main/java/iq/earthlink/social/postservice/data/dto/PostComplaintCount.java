package iq.earthlink.social.postservice.data.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@ApiModel(description = "Post info with complaints counter")
public class PostComplaintCount {
    @ApiModelProperty("Post id")
    private long id;
    @ApiModelProperty("Post title")
    private String title;
    @ApiModelProperty("Post content")
    private String content;
    @ApiModelProperty("Post author")
    private String authorId;
    @ApiModelProperty("Post group id")
    private Long userGroupId;
    @ApiModelProperty("Count of complaints made by users")
    private long complaintsCount;
}
