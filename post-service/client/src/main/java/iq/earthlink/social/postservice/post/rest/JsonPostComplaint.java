package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.AuthorDetails;
import iq.earthlink.social.postservice.post.PostComplaintState;
import lombok.Data;

import java.util.Date;

@Data
public class JsonPostComplaint {
  private Long id;
  private Long postId;
  @ApiModelProperty(value = "The timestamp when complaint has been created", dataType = "Long.class", example = "123352312352")
  private Date createdAt;
  private Long authorId;
  private AuthorDetails author;
  private String reasonOther;
  private JsonReason reason;
  private PostComplaintState state;

  @ApiModelProperty("The person id who resolved the complaint")
  private Long resolverId;
  private String resolvingText;
  @ApiModelProperty(value = "The timestamp when complaint has been resolved", dataType = "Long.class", example = "123352312352")
  private Date resolvingDate;
}