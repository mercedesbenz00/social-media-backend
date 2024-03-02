package iq.earthlink.social.postservice.post.collection;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostCollectionSearchCriteria {
  private Long personId;
  private String query;
}
