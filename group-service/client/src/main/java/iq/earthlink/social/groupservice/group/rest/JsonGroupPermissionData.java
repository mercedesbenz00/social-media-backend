package iq.earthlink.social.groupservice.group.rest;

import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.GroupPermissionData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonGroupPermissionData implements GroupPermissionData {

  @NotNull
  private Long personId;

  @NotNull
  private GroupMemberStatus permission;

}
