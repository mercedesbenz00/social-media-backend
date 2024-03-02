package iq.earthlink.social.personservice.person;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowSearchCriteria {

  private Long personId;
  private String query;
  private Long[] followingIds;
  private Long[] followerIds;
}
