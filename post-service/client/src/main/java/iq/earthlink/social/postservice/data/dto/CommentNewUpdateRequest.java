package iq.earthlink.social.postservice.data.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * DTO for create/update comments
 */
@Data
@ApiModel(description = "Request for creating/updating comments")
public class CommentNewUpdateRequest {

    /**
     * Content of comment
     */
    @ApiModelProperty("Content of comment")
    private String content;

    @ApiModelProperty("Reference to a persons that mentioned in the comment")
    private String[] personReferences;
}
