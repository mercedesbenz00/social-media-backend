package iq.earthlink.social.personservice.person.model;

import iq.earthlink.social.personservice.person.impl.repository.ComplaintRepository;

import java.util.Date;

/**
 * The projection interface for the
 * {@link ComplaintRepository#getPersonComplainStats(Long)}  method.
 */
public interface PersonComplaintStats {
    Long getComplaintCount();
    Date getLastComplaintDate();
}
