package iq.earthlink.social.groupservice.group.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.groupservice.group.rest.enumeration.AccessType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ApiModel(description = "User Group Permission DTO")
@FieldNameConstants
public class UserGroupPermissionDto {

    @ApiModelProperty("The group identifier")
    private Long id;

    @ApiModelProperty("The group name")
    private String name;

    @ApiModelProperty("The group avatar")
    private JsonMediaFile avatar;

    @ApiModelProperty("User group access type")
    private AccessType accessType;

    @ApiModelProperty("User group statistic object, contains published posts counts, pending posts Count, member counts, " +
            "pending join requests, score")
    private JsonUserGroupStats stats;

}
