package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.enumeration.CommentComplaintState;
import lombok.Data;

import java.util.Date;

@Data
public class JsonCommentComplaint {
  private Long id;
  private String complaintUuid;
  private Long commentId;
  private Long authorId;
  @ApiModelProperty(value = "The timestamp when complaint has been created", dataType = "Long.class", example = "123352312352")
  private Date createdAt;
  private String reasonOther;
  private JsonReason reason;
  private CommentComplaintState state;

  @ApiModelProperty("The person id who resolved the complaint")
  private Long resolverId;
  private String resolvingText;
  @ApiModelProperty(value = "The timestamp when complaint has been resolved", dataType = "Long.class", example = "123352312352")
  private Date resolvingDate;
}
