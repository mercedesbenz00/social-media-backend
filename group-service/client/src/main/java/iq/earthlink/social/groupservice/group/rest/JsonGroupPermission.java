package iq.earthlink.social.groupservice.group.rest;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JsonGroupPermission {
  private Long id;
  private UserGroupPermissionDto userGroup;
  private Long personId;
  private Long authorId;
  private GroupMemberStatus permission;
  private PersonData person;
}
