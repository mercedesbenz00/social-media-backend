package iq.earthlink.social.postservice.post.comment;

import iq.earthlink.social.classes.data.dto.ContentModerationDto;
import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.enumeration.CommentComplaintState;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.postservice.group.dto.GroupDTO;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CommentData;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.JsonComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface CommentService {

  @Nonnull
  JsonComment createComment(
          String authorizationHeader,
          @Nonnull PersonDTO author,
          @Nonnull CommentData commentData);

  @Nonnull
  JsonComment createCommentWithFile(
          String authorizationHeader,
          @Nonnull PersonDTO author,
          @Nonnull CommentData commentData,
          MultipartFile file);

  @Nonnull
  JsonComment getCommentWithReplies(Long personId, @Nonnull UUID commentUuid);

  @Nonnull
  Comment getCommentByUuidInternal(@Nonnull UUID commentUuid);

  @Nonnull
  Page<JsonComment> findComments(@Nonnull Long personId, @Nonnull Boolean isAdmin, @Nonnull UUID postUuid, Boolean showAll, @Nonnull Pageable page);

  List<JsonComment> getCommentsInternal(@Nonnull Long postId);

  @Nonnull
  Page<JsonComment> findCommentsWithComplaints(@Nonnull PersonDTO person, Long groupId,
                                           CommentComplaintState complainStatus, boolean deleted, @Nonnull Pageable page);

  @Nonnull
  JsonComment reply(
          @Nonnull String authorizationHeader,
          @Nonnull PersonDTO replyAuthor,
          @Nonnull UUID sourceCommentUuid,
          @Nonnull CommentData data,
          MultipartFile file);

  @Nonnull
  JsonComment edit(
          String authorizationHeader, @Nonnull PersonDTO editor,
          @Nonnull UUID commentUuid,
          @Nonnull CommentData data,
          MultipartFile file);

  @Nonnull
  JsonComment editWithFile(
          String authorizationHeader, @Nonnull PersonDTO editor,
          @Nonnull UUID commentUuid,
          @Nonnull CommentData data,
          MultipartFile file);

  void rejectCommentByComplaint(
          @Nonnull PersonDTO requester,
          @Nonnull String reason,
          @Nonnull UUID complaintUuid);

  void removeComment(PersonDTO requester, @Nonnull UUID commentUuid);

  @Nonnull
  Page<JsonComment> getReplies(@Nonnull Long personId, @Nonnull Boolean isAdmin, UUID commentUuid, Boolean showAll, Pageable page);

  void removeCommentByModerator(ContentModerationDto dto);

  void removePostComments(PersonDTO person, Long postId);

  void sendNotification(PersonData person, @Nonnull GroupDTO group, @Nonnull Post post,
                        @Nonnull Comment comment, @Nonnull NotificationType type, CommentData commentData,
                        Map<String, String> additionalMetadata);

  void sendNotification(PersonData person, @Nonnull GroupDTO group, @Nonnull Post post,
                        @Nonnull Comment comment, Comment reply, @Nonnull NotificationType type, CommentData commentData,
                        Map<String, String> additionalMetadata, Set<Long> receiverIds);

  void deleteMediaFile(String authorizationHeader, PersonDTO person, UUID commentUuid);
}
