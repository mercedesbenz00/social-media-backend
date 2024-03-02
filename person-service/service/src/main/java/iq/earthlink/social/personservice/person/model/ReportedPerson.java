package iq.earthlink.social.personservice.person.model;

import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.personservice.dto.PersonStatus;
import iq.earthlink.social.personservice.person.impl.repository.ComplaintRepository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * The projection interface for the
 * {@link ComplaintRepository#findPersonsWithComplaints(PersonComplaint.PersonComplaintState, List, PersonStatus, LocalDate, LocalDate, Pageable)} method.
 */
public interface ReportedPerson {

    Long getId();
    String getDisplayName();
    MediaFile getAvatar();
    Date getCreatedAt();
    Long getPostCount();
    Boolean getIsBanned();
    Date getBanExpiresAt();

}
