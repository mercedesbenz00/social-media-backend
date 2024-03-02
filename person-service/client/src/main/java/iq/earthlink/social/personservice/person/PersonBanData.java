package iq.earthlink.social.personservice.person;

import java.util.List;

public interface PersonBanData {
    Long getReasonId();
    String getReason();
    Long getPersonId();
    List<Long> getGroupIds();
    int getDays();
}
