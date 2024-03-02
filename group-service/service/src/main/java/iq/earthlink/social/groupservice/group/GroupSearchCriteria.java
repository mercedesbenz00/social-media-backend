package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupSearchCriteria {

  private Long memberId;
  private String query;
  private Boolean isAdmin;
  private Long[] categoryIds;
  private Long[] groupIds;
  private List<Long> subscribedToIds;
  private final List<ApprovalState> states;
  private final GroupMemberStatus status;
  @Builder.Default
  private Double similarityThreshold = 0.2;
}
