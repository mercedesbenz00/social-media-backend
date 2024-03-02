package iq.earthlink.social.personservice.person;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BanSearchCriteria {
    private String query;
    private Long bannedPersonId;
    private Long[] bannedPersonIds;
    private List<Long> groupIds;
    private Boolean active;
}
