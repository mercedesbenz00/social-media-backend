package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.AuthorDetails;
import iq.earthlink.social.classes.data.dto.CommentDetailStatistics;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class JsonComment {

  private String commentUuid;

  private Long id;

  private String content;

  @ApiModelProperty("The person id who created the comment")
  private Long authorId;

  @ApiModelProperty(value = "The timestamp when comment has been created", dataType = "Long.class", example = "1234354354112")
  private Date createdAt;

  @ApiModelProperty(value = "The timestamp when comment has been changed", dataType = "Long.class", example = "1234354354112")
  private Date modifiedAt;

  @ApiModelProperty("The person id who edited the comment")
  private Long modifiedBy;

  @ApiModelProperty("The post identifier for which this comment belongs")
  private Long postId;

  @ApiModelProperty("The group identifier for which this comment belongs")
  private Long userGroupId;

  @ApiModelProperty(value = "The comment uuid for which comment is a reply", dataType = "UUID.class", example = "eb179fc8-a7b5-468a-b41f-18cda51d1485")
  private UUID replyTo;

  private JsonVoteCount totalVotes;

  private Long replyCommentsCount;

  private JsonMediaFile file;

  private boolean isDeleted;

  private List<JsonComment> topReplies;

  private AuthorDetails author;

  private CommentDetailStatistics stats;

  public boolean isEditedByAuthor() {
    return getModifiedAt().after(getCreatedAt()) && getAuthorId().equals(getModifiedBy());
  }
}
