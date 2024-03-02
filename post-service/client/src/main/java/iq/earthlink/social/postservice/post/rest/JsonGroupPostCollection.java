package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class JsonGroupPostCollection {
  private Long id;
  private Long groupId;
  private Long authorId;
  @ApiModelProperty("Indicates if the collection is the default for the group")
  private boolean defaultCollection;
  private String name;
  @ApiModelProperty(value = "The timestamp when group post collection has been created", dataType = "Long.class", example = "123352312352")
  private Date createdAt;
  private List<JsonPost> posts;
}
