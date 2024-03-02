package iq.earthlink.social.groupservice.group;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategorySearchCriteria {

    private Long personId;
    private String query;
    private Long parentCategoryId;
    private String locale;
    private boolean skipParent;
    private boolean skipUnusedInGroups;
    @Builder.Default
    private Double similarityThreshold = 0.2;
}
