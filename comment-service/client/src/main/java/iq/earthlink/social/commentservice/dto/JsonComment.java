package iq.earthlink.social.commentservice.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class JsonComment {
    private Long id;
    private String content;

    @ApiModelProperty("The person id who created the comment")
    private Long authorId;

    @ApiModelProperty(value = "The timestamp when comment has been created", dataType = "Long.class", example = "1234354354112")
    private Date createdAt;

    @ApiModelProperty(value = "The timestamp when comment has been changed", dataType = "Long.class", example = "1234354354112")
    private Date modifiedAt;

    @ApiModelProperty("The unique identifier, for which this comment belongs")
    private UUID objectId;

    @ApiModelProperty(value = "The comment id for which comment is a reply", dataType = "Long.class", example = "1234354354112")
    private Long replyTo;

    private Long replyCommentsCount;

    private boolean isDeleted;
}
