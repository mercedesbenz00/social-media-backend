package iq.earthlink.social.postservice.post.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.validation.NewEntityGroup;
import iq.earthlink.social.postservice.post.PostComplaintData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JsonPostComplaintData implements PostComplaintData {

  @ApiModelProperty("The description about complaint")
  @NotBlank(groups = NewEntityGroup.class)
  private JsonReason reason;
  private String reasonOther;
}
