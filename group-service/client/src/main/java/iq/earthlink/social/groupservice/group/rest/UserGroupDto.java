package iq.earthlink.social.groupservice.group.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.rest.enumeration.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ApiModel(description = "User Group DTO")
@FieldNameConstants
public class UserGroupDto  {

  @ApiModelProperty("The group identifier")
  private Long id;

  @ApiModelProperty("The person id who created the group")
  private Long ownerId;

  @ApiModelProperty("The group name")
  private String name;

  @ApiModelProperty("The group description")
  private String description;

  @ApiModelProperty("The group rules description")
  private String rules;

  @ApiModelProperty(value = "The timestamp when group has been created", dataType = "Long.class", example = "1234352343123")
  private Date createdAt;

  @ApiModelProperty("The categories list assigned to the group")
  private List<JsonCategory> categories;

  @ApiModelProperty("The tag list assigned to the group")
  private List<JsonTag> tags;

  @ApiModelProperty("User group statistic object, contains published posts counts, member counts, score")
  private JsonUserGroupStats stats;

  @ApiModelProperty("User group access type")
  private AccessType accessType;

  @ApiModelProperty("User group posting permission")
  private PostingPermission postingPermission;

  @ApiModelProperty("User group invite permission")
  private InvitePermission invitePermission;

  @ApiModelProperty("User group approval state")
  private ApprovalState state;

  @ApiModelProperty("Group member approval state for the logged-in user")
  private ApprovalState memberState;

  @ApiModelProperty("The Cover image for the group")
  private JsonMediaFile cover;

  @ApiModelProperty("The Avatar image for the group")
  private JsonMediaFile avatar;

  /**
   * Indicates, who can see the group info - everyone or invited people.
   */
  private GroupVisibility visibility;

  @ApiModelProperty(value = "Group permissions of the logged-in user")
  private Set<GroupMemberStatus> permissions;

  @ApiModelProperty(value = "List of group moderators")
  private List<JsonGroupPermission> groupModerators;
}
