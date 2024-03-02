package iq.earthlink.social.groupservice.group.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonCategory {
  private Long id;
  private UUID categoryUUID;
  private String name;
  private JsonCategory parentCategory;
  private Long groupCount;
  private Long personCount;
  private JsonMediaFile cover;
  private JsonMediaFile avatar;

  @ApiModelProperty(
      value = "The timestamp when category was created",
      dataType = "Long.class",
      example = "123123535234")
  private Date createdAt;
}
