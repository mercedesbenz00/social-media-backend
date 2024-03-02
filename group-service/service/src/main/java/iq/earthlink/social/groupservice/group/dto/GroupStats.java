package iq.earthlink.social.groupservice.group.dto;

import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.groupservice.group.rest.CreateGroupRequests;
import iq.earthlink.social.groupservice.group.rest.CreatedGroups;
import iq.earthlink.social.groupservice.group.rest.JoinGroupRequests;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupStats {

  private long allGroupsCount;
  private long newGroupsCount;
  private List<CreatedGroups> createdGroups;
  private List<CreateGroupRequests> createGroupRequests;
  private List<JoinGroupRequests> joinGroupRequests;
  private TimeInterval timeInterval;
  private Timestamp fromDate;
}
