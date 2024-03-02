package iq.earthlink.social.postservice.post;

import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.classes.enumeration.SortType;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
public class PostSearchCriteria {
    private String query;
    private Long userId;
    private final Collection<Long> groupIds;
    private final Collection<Long> postIds;
    private Collection<PostState> states;
    private Collection<Long> authorIds;
    private final Boolean pinned;
    private final SortType sortType;
    @Builder.Default
    private Double similarityThreshold = 0.2;
}
