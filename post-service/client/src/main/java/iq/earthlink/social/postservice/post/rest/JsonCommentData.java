package iq.earthlink.social.postservice.post.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.validation.NewEntityGroup;
import iq.earthlink.social.postservice.post.CommentData;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Data
public class JsonCommentData implements CommentData {

  @ApiModelProperty("The related post uuid")
  @NotNull(groups = NewEntityGroup.class)
  private UUID postUuid;

  @ApiModelProperty("The text content of the comment")
  private String content;

  @ApiModelProperty("IDs of persons mentioned in the comment")
  private List<Long> mentionedPersonIds;

  @JsonIgnore
  private boolean allowEmptyContent;
}
