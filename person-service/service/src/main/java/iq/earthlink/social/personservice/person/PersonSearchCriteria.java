package iq.earthlink.social.personservice.person;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PersonSearchCriteria {

  private String query;
  private String displayNameQuery;
  private Long currentPersonId;
  private Long[] personIds;
  private Long[] personIdsToExclude;
  private boolean followingsFirst;
  private Boolean showDeleted;
  @Builder.Default
  private Double similarityThreshold = 0.2;
}
