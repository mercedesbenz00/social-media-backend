package iq.earthlink.social.postservice.post.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@ApiModel(description = "The person post collection model")
@Data
public class JsonPostCollection {
  private Long id;
  private Long ownerId;
  @JsonProperty("isPublic")
  private boolean isPublic;
  private String name;
  @ApiModelProperty(value = "The timestamp when collection has been created", dataType = "Long.class", example = "123352312352")
  private Date createdAt;
  private List<JsonPost> posts;
}
