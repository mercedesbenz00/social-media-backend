package iq.earthlink.social.shortvideoregistryservice.dto;

import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShortVideoSearchCriteria {
    private Long groupId;
    private String title;
    private Long authorId;
    private PrivacyLevel privacyLevel;
    private boolean commentsAllowed;
}
