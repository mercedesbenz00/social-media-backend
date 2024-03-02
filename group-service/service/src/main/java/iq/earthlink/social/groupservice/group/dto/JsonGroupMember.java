package iq.earthlink.social.groupservice.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.groupservice.person.dto.PersonDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonGroupMember {
  private Long id;
  private Long personId;
  private Long groupId;
  private String displayName;
  private PersonDTO person;
  private String state;
  @JsonProperty("isFollowing")
  private boolean isFollowing;

  @ApiModelProperty(value = "The timestamp when person become a group member", dataType = "Long.class", example = "1231434533254")
  private Date createdAt;
}
