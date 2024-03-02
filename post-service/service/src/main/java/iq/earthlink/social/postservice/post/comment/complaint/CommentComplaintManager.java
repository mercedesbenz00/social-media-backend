package iq.earthlink.social.postservice.post.comment.complaint;

import iq.earthlink.social.classes.data.dto.ComplaintRequest;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CommentComplaintData;
import iq.earthlink.social.postservice.post.rest.JsonCommentComplaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;
import java.util.UUID;

public interface CommentComplaintManager {

    @Nonnull
    JsonCommentComplaint createComplaint(
            @Nonnull Long personId,
            @Nonnull UUID commentUuid,
            @Nonnull CommentComplaintData data
    );

    @Nonnull
    JsonCommentComplaint createComplaint(
            @Nonnull Long personId,
            @Nonnull UUID commentUuid,
            @Nonnull ComplaintRequest request);

    @Nonnull
    Page<CommentComplaint> findComplaints(
            @Nonnull PersonDTO person,
            @Nonnull UUID commentUuid,
            @Nonnull Pageable page);

    @Nonnull
    CommentComplaint updateComplaint(
            @Nonnull Long personId,
            @Nonnull UUID commentUuid,
            @Nonnull UUID complaintUuid,
            @Nonnull CommentComplaintData data
    );

    @Nonnull
    CommentComplaint updateComplaint(
            @Nonnull Long personId,
            @Nonnull UUID commentUuid,
            @Nonnull UUID complaintUuid,
            @Nonnull ComplaintRequest request);

    @Nonnull
    CommentComplaint getComplaint(Long personId, UUID commentUuid, UUID complaintUuid);

    void removeComplaint(Long personId, UUID commentUuid, UUID complaintUuid);

    void rejectAllComplaints(PersonDTO person, String reason, UUID commentUuid);
}
