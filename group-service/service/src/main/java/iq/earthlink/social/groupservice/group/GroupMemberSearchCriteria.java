package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import lombok.Builder;
import lombok.Data;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.List;

@Data
@Builder
public class GroupMemberSearchCriteria {

  private final long groupId;
  private List<Long> personIds;
  private String query;
  @Enumerated(EnumType.STRING)
  private List<ApprovalState> states;
}
