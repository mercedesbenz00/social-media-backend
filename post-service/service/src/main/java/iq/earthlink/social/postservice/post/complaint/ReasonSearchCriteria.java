package iq.earthlink.social.postservice.post.complaint;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReasonSearchCriteria {
    private String query;
    private String locale;
}
