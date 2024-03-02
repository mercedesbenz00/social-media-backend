package iq.earthlink.social.commentservice.service;

import iq.earthlink.social.classes.enumeration.CommentComplaintState;
import iq.earthlink.social.commentservice.dto.CommentData;
import iq.earthlink.social.commentservice.repository.CommentEntityRepository;
import iq.earthlink.social.common.data.model.CommentEntity;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.personservice.person.PersonInfo;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentEntityRepository commentEntityRepository;

    public CommentServiceImpl(
            CommentEntityRepository commentEntityRepository) {
        this.commentEntityRepository = commentEntityRepository;
    }

    @NonNull
    @Override
    public CommentEntity createComment(@Nonnull PersonInfo requester, @NonNull CommentData commentData) {
        CommentEntity c = CommentEntity.builder()
                .content(commentData.getContent())
                .authorId(requester.getId())
                .objectId(commentData.getObjectId())
                .build();
        c = commentEntityRepository.saveAndFlush(c);

        return c;
    }

    @NonNull
    @Override
    public CommentEntity updateComment(@Nonnull PersonInfo requester, @NonNull Long commentId, @NonNull CommentData commentData) {
        CommentEntity comment = commentEntityRepository.findByIdAndObjectIdAndAuthorId(commentId, commentData.getObjectId(), requester.getId())
                .orElseThrow(() -> new NotFoundException("error.not.found.comment", commentId));

        comment.setContent(commentData.getContent());

        return commentEntityRepository.save(comment);
    }

    @Nonnull
    @Override
    public CommentEntity getCommentEntity(@Nonnull Long commentId, @Nonnull UUID objectId) {
        return commentEntityRepository.findByIdAndObjectId(commentId, objectId)
                .orElseThrow(() -> new NotFoundException("error.not.found.comment", commentId));
    }

    @NonNull
    @Override
    public Page<CommentEntity> findComments(@Nonnull PersonInfo requester, @NonNull UUID objectId, Boolean showAll, @NonNull Pageable page) {
        return commentEntityRepository.findComments(objectId, showAll, page);
    }

    @NonNull
    @Override
    public Page<CommentEntity> findCommentsWithComplaints(@Nonnull PersonInfo requester, Long groupId, CommentComplaintState complainStatus, boolean deleted, @NonNull Pageable page) {
        //not implemented yet
        return Page.empty(page);
    }

    @NonNull
    @Override
    public CommentEntity reply(@Nonnull PersonInfo requester, @NonNull Long sourceCommentId, @NonNull CommentData data) {

        CommentEntity sourceComment = getCommentEntity(sourceCommentId, data.getObjectId());

        CommentEntity reply = CommentEntity.builder()
                .authorId(data.getAuthorId())
                .content(data.getContent())
                .replyTo(sourceComment)
                .objectId(sourceComment.getObjectId())
                .build();

        return commentEntityRepository.save(reply);
    }

    @Override
    public void rejectCommentByComplaint(@NonNull PersonInfo requester, @NonNull String reason, @NonNull Long complaintId) {
        //not implemented yet
    }

    @Override
    public void removeComment(@NonNull PersonInfo requester, @NonNull Long commentId, @Nonnull UUID objectId) {
        CommentEntity comment = getCommentEntity(commentId, objectId);
        if (canChange(requester, comment)) {
            remove(comment);
        } else {
            throw new ForbiddenException("error.person.can.not.delete.comment");
        }
    }

    @NonNull
    @Override
    public Page<CommentEntity> getReplies(@NonNull PersonInfo requester, Long commentId, UUID objectId, Boolean showAll, Pageable page) {
        if (commentId == null) {
            return Page.empty(page);
        }

        return commentEntityRepository.findReplies(commentId, objectId, showAll, page);
    }

    private boolean canChange(PersonInfo requester, CommentEntity comment) {
        return Objects.equals(requester.getId(), comment.getAuthorId());
    }

    private void remove(CommentEntity comment) {
        commentEntityRepository.delete(comment);
    }

}
