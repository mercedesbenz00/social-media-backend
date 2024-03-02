package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.validation.NewEntityGroup;
import iq.earthlink.social.postservice.post.PostCollectionData;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JsonPostCollectionData implements PostCollectionData {

  @ApiModelProperty("The name of the post collection")
  @NotBlank(groups = NewEntityGroup.class)
  private String name;

  @ApiModelProperty("The flag that indicates if the post collection is public")
  private Boolean isPublic;
}
