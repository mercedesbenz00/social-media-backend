package iq.earthlink.social.postservice.post.comment.complaint;

import iq.earthlink.social.classes.data.dto.ComplaintRequest;
import iq.earthlink.social.classes.enumeration.CommentComplaintState;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CommentComplaintData;
import iq.earthlink.social.postservice.post.comment.Comment;
import iq.earthlink.social.postservice.post.comment.CommentService;
import iq.earthlink.social.postservice.post.comment.complaint.repository.CommentComplaintRepository;
import iq.earthlink.social.postservice.post.complaint.ReasonManager;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import iq.earthlink.social.postservice.post.rest.JsonCommentComplaint;
import iq.earthlink.social.postservice.post.rest.JsonCommentComplaintData;
import iq.earthlink.social.postservice.util.PermissionUtil;
import lombok.RequiredArgsConstructor;
import org.dozer.Mapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Nonnull;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DefaultCommentComplaintManager implements CommentComplaintManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCommentComplaintManager.class);

    private final CommentComplaintRepository repository;
    private final ReasonManager reasonManager;
    private final PermissionUtil permissionUtil;
    private final CommentService commentService;
    private final Mapper mapper;

    @Nonnull
    @Override
    public JsonCommentComplaint createComplaint(
            @Nonnull Long personId,
            @Nonnull UUID commentUuid,
            @Nonnull CommentComplaintData data) {
        Comment comment = commentService.getCommentByUuidInternal(commentUuid);
        checkReason(comment.getId(), data);

        if (comment.isDeleted()) {
            throw new ForbiddenException("error.comment.complaint.comment.deleted");
        }

        Optional<CommentComplaint> complaintOpt = repository.findByAuthorIdAndCommentId(personId, comment.getId());
        if (complaintOpt.isPresent()) {
            throw new NotUniqueException("error.comment.complaint.already.created");
        }
        // Find predefined reason (if any):
        Reason reason = Objects.nonNull(data.getReason()) ? reasonManager.getComplaintReason(data.getReason().getId()) : null;

        CommentComplaint complaint = CommentComplaint.builder()
                .comment(comment)
                .authorId(personId)
                .reason(reason)
                .reasonOther(data.getReasonOther())
                .state(CommentComplaintState.PENDING)
                .build();

        CommentComplaint commentComplaint = repository.save(complaint);
        return mapper.map(commentComplaint, JsonCommentComplaint.class);
    }

    @NotNull
    @Override
    public JsonCommentComplaint createComplaint(
            @NotNull Long personId,
            @NotNull UUID commentUuid,
            @NotNull ComplaintRequest request) {
        JsonCommentComplaintData data = mapper.map(request, JsonCommentComplaintData.class);
        if (request.getReasonId() == null) {
            data.setReason(null);
        }
        return createComplaint(personId, commentUuid, data);
    }

    @Nonnull
    @Override
    public Page<CommentComplaint> findComplaints(
            @Nonnull PersonDTO person,
            @Nonnull UUID commentUuid,
            @Nonnull Pageable page) {
        Comment comment = commentService.getCommentByUuidInternal(commentUuid);

        permissionUtil.checkGroupPermissions(person, comment.getPost().getUserGroupId());

        return repository.findComplaints(comment.getId(), page);
    }

    @Nonnull
    @Override
    public CommentComplaint updateComplaint(
            @Nonnull Long personId,
            @Nonnull UUID commentUuid,
            @Nonnull UUID complaintUuid,
            @Nonnull CommentComplaintData data) {
        Comment comment = commentService.getCommentByUuidInternal(commentUuid);
        checkReason(comment.getId(), data);
        CommentComplaint complaint = getComplaint(personId, comment.getCommentUuid(), complaintUuid);

        // Find predefined reason (if any):
        Reason reason = Objects.nonNull(data.getReason()) ? reasonManager.getComplaintReason(data.getReason().getId()) : null;

        complaint.setReason(reason);
        complaint.setReasonOther(data.getReasonOther());

        return repository.save(complaint);
    }

    @NotNull
    @Override
    public CommentComplaint updateComplaint(
            @NotNull Long personId,
            @NotNull UUID commentUuid,
            @NotNull UUID complaintUuid,
            @NotNull ComplaintRequest request) {
        JsonCommentComplaintData data = mapper.map(request, JsonCommentComplaintData.class);
        if (request.getReasonId() == null) {
            data.setReason(null);
        }
        return updateComplaint(personId, commentUuid, complaintUuid, data);
    }

    @Nonnull
    @Override
    public CommentComplaint getComplaint(Long personId, UUID commentUuid, UUID complaintUuid) {
        return repository.findByComplaintUuid(complaintUuid)
                .filter(c -> c.getComment().getCommentUuid().equals(commentUuid) && c.getAuthorId()
                        .equals(personId))
                .orElseThrow(() -> new NotFoundException("error.not.found.complaint", complaintUuid));
    }

    @Override
    public void removeComplaint(Long personId, UUID commentUuid, UUID complaintUuid) {
        repository.delete(getComplaint(personId, commentUuid, complaintUuid));
    }

    @Override
    public void rejectAllComplaints(PersonDTO person, String reason, UUID commentUuid) {
        Comment comment = commentService.getCommentByUuidInternal(commentUuid);

        permissionUtil.checkGroupPermissions(person, comment.getPost().getUserGroupId());

        LOGGER.debug("Rejecting all complaints for the comment with id: {}, rejecting by: {}",
                comment.getId(), person.getPersonId());

        List<CommentComplaint> commentComplaints = repository.findByCommentIdAndState(comment.getId(), CommentComplaintState.PENDING);

        if (!CollectionUtils.isEmpty(commentComplaints)) {
            commentComplaints.forEach(commentComplaint -> {
                commentComplaint.setResolverId(person.getPersonId());
                commentComplaint.setResolvingText(reason);
                commentComplaint.setResolvingDate(new Date());
                commentComplaint.setState(CommentComplaintState.REJECTED);
            });
            repository.saveAll(commentComplaints);
        }
    }

    private void checkReason(Long commentId, CommentComplaintData data) {
        if (ObjectUtils.isEmpty(data.getReason()) && ObjectUtils.isEmpty(data.getReasonOther())) {
            throw new BadRequestException("error.comment.complaint.reason.not.provided", commentId);
        }

    }
}
