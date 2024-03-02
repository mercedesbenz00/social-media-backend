package iq.earthlink.social.postservice.post;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollectionPostsSearchCriteria {
    private final Long postId;
    private final String query;
}
