package iq.earthlink.social.personservice.person;

import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonComplaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.List;

public interface ComplaintManager {

  /**
   * Creates new complaint initiated by the complaint owner against violator.
   * @param authorizationHeader - authorization header
   * @param currentUser the complaint's owner person
   * @param data complaint data containing violator ID, user group ID, reason
   */
  @Nonnull
  PersonComplaint createComplaint(String authorizationHeader, @Nonnull Person currentUser, @Nonnull ComplaintData data);

  /**
   * Finds complaints created by the owner person.
   * @param currentUser - logged-in user
   * @param groupIds - list of group IDs
   * @param personIds - list of person IDs
   * @param state - person complaint state
   * @param page the pagination params
   */
  @Nonnull
  Page<PersonComplaint> findComplaints(String authorizationHeader, Person currentUser, List<Long> groupIds,
                                       List<Long> personIds, PersonComplaint.PersonComplaintState state,
                                       @Nonnull Pageable page);

  /**
   * Removes existent complaint created earlier by the owner person agains violator person.
   *
   * @param currentUser the complaint's owner person
   * @param complaintId the complaint identifier
   */
  void removeComplaint(@Nonnull Person currentUser, @Nonnull Long complaintId);

  @Transactional
  void moderate(String authorizationHeader, Person currentUser, Long complaintId, String reason,
                PersonComplaint.PersonComplaintState state, boolean resolveAll);
}
