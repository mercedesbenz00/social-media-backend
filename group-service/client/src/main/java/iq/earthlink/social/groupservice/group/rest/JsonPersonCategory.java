package iq.earthlink.social.groupservice.group.rest;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.Set;

@Data
public class JsonPersonCategory {
  @ApiModelProperty("The person category identifier")
  private Long id;

  @ApiModelProperty("The set of the categories person interested in")
  private Set<JsonCategory> categories;

  @ApiModelProperty("The person identifier")
  private Long personId;

  @ApiModelProperty(value = "The timestamp when person category (interest) has been created", dataType = "Long.class", example = "21345435233214")
  private Date createdAt;
}
