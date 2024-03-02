package iq.earthlink.social.commentservice.service;

import iq.earthlink.social.classes.enumeration.CommentComplaintState;
import iq.earthlink.social.commentservice.dto.CommentData;
import iq.earthlink.social.common.data.model.CommentEntity;
import iq.earthlink.social.personservice.person.PersonInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;
import java.util.UUID;

public interface CommentService {
    @Nonnull
    CommentEntity createComment(@Nonnull PersonInfo requester, @Nonnull CommentData commentData);

    @Nonnull
    CommentEntity updateComment(@Nonnull PersonInfo requester, @Nonnull Long commentId, @Nonnull CommentData commentData);

    @Nonnull
    CommentEntity getCommentEntity(@Nonnull Long commentId, @Nonnull UUID objectId);

    @Nonnull
    Page<CommentEntity> findComments(@Nonnull PersonInfo requester, @Nonnull UUID objectId, Boolean showAll, @Nonnull Pageable page);

    @Nonnull
    Page<CommentEntity> findCommentsWithComplaints(@Nonnull PersonInfo requester, Long groupId, CommentComplaintState complainStatus,
                                                   boolean deleted, @Nonnull Pageable page);

    @Nonnull
    CommentEntity reply(
            @Nonnull PersonInfo requester,
            @Nonnull Long sourceCommentId,
            @Nonnull CommentData data);

    void rejectCommentByComplaint(
            @Nonnull PersonInfo requester,
            @Nonnull String reason,
            @Nonnull Long complaintId);

    void removeComment(
            @Nonnull PersonInfo requester,
            @Nonnull Long commentId,
            @Nonnull UUID objectId);

    @Nonnull
    Page<CommentEntity> getReplies(@Nonnull PersonInfo requester, Long commentId, UUID objectId, Boolean showAll, Pageable page);
}
