package iq.earthlink.social.postservice.post.complaint;

import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import iq.earthlink.social.postservice.post.rest.ReasonRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;

public interface ReasonManager {

  Reason createComplaintReason(ReasonRequest data, PersonDTO person);

  Reason updateComplaintReason(@Nonnull Long reasonId, ReasonRequest data, PersonDTO person);

  Reason getComplaintReason(@Nonnull Long reasonId);

  Page<Reason> findComplaintReasons(ReasonSearchCriteria criteria, Pageable page);

  void removeComplaintReason(@Nonnull Long reasonId, PersonDTO person);
}
