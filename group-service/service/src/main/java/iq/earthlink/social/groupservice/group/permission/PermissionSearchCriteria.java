package iq.earthlink.social.groupservice.group.permission;

import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PermissionSearchCriteria {
  private List<Long> groupIds;
  private Long personId;
  private List<GroupMemberStatus> statuses;
  private String query;
  @Builder.Default
  private Double similarityThreshold = 0.2;
}
