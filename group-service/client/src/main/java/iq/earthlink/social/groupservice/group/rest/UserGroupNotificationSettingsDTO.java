package iq.earthlink.social.groupservice.group.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "User Group Notification Settings DTO")
@FieldNameConstants
@Builder
public class UserGroupNotificationSettingsDTO {
    @ApiModelProperty("Group id")
    private Long groupId;

    @ApiModelProperty("Is this group muted")
    @JsonProperty("isMuted")
    private boolean isMuted;
}
