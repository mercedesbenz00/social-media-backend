package iq.earthlink.social.postservice.post.complaint;

import iq.earthlink.social.classes.data.dto.ComplaintRequest;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.PostComplaintData;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.JsonPostComplaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostComplaintManager {

  JsonPostComplaint createComplaint(Long personId, Post post, PostComplaintData data);

  JsonPostComplaint createComplaint(Long personId, Post post, ComplaintRequest request);

  JsonPostComplaint getJsonPostComplaint(Long personId, Post post, Long complaintId);

  Page<JsonPostComplaint> findComplaints(PersonDTO person, Post post, Pageable page);

  JsonPostComplaint updateComplaint(Long personId, Post post, Long complaintId, PostComplaintData data);

  JsonPostComplaint updateComplaint(Long personId, Post post, Long complaintId, ComplaintRequest request);

  void removeComplaint(Long personId, Post post, Long complaintId);

  void rejectAllComplaints(PersonDTO person, String reason, Long postId);
}
