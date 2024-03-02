package iq.earthlink.social.groupservice.group.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.groupservice.group.GroupData;
import iq.earthlink.social.groupservice.group.rest.enumeration.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Pattern;
import java.util.Set;

@Data
@NoArgsConstructor
public class JsonGroupData implements GroupData {

    @Length(max = 255, min = 2)
    @Pattern(regexp = "^[\\u0621-\\u064AA-Za-z_\\-][\\u0621-\\u064AA-Za-z0-9 _\\-]+$")
    @ApiModelProperty("The group name")
    private String name;

    @Length(max = 2000)
    @ApiModelProperty("The group description")
    private String description;

    @Length(max = 4000)
    @ApiModelProperty("The group rules description")
    private String rules;

    @ApiModelProperty("Categories ids assigned to the group")
    private Set<Long> categories;

    @ApiModelProperty("Tag ids assigned to the group")
    private Set<Long> tags;

    @ApiModelProperty(value = "User group access type", example = "'PRIVATE', or 'PUBLIC'")
    private AccessType accessType;

    public void setAccessType(String accessType) {
        this.accessType = AccessType.valueOf(accessType.toUpperCase());
    }

    @ApiModelProperty(value = "User group posting permission", example = "'ADMIN_ONLY', 'WITH_APPROVAL', or 'ALL'")
    private PostingPermission postingPermission;

    @ApiModelProperty(value = "User group invite permission", example = "'ADMIN', 'MEMBER'")
    private InvitePermission invitePermission;

    public void setPostingPermission(String postingPermission) {
        this.postingPermission = PostingPermission.valueOf(postingPermission.toUpperCase());
    }

    public void setInvitePermission(String invitePermission) {
        this.invitePermission = InvitePermission.valueOf(invitePermission.toUpperCase());
    }

    @ApiModelProperty("The group state property can be used only on group update and only by ADMIN user or group moderator/admin")
    private ApprovalState state;

    @ApiModelProperty(value="Indicates, who can see the group info - everyone or invited people.", example="EVERYONE or INVITED")
    private GroupVisibility visibility;
}
