package iq.earthlink.social.postservice.post.comment;

import iq.earthlink.social.classes.data.dto.*;
import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.*;
import iq.earthlink.social.common.audit.AuditMarker;
import iq.earthlink.social.common.audit.AuditMessage;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.common.data.event.PostActivityEvent;
import iq.earthlink.social.common.util.CommonUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.personservice.rest.PersonBanRestService;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.postservice.event.KafkaProducerService;
import iq.earthlink.social.postservice.event.KafkaTopics;
import iq.earthlink.social.postservice.group.GroupManager;
import iq.earthlink.social.postservice.group.dto.GroupDTO;
import iq.earthlink.social.postservice.group.member.GroupMemberManager;
import iq.earthlink.social.postservice.person.PersonManager;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CommentData;
import iq.earthlink.social.postservice.post.comment.complaint.CommentComplaint;
import iq.earthlink.social.postservice.post.comment.complaint.repository.CommentComplaintRepository;
import iq.earthlink.social.postservice.post.comment.repository.CommentRepository;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettings;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettingsRepository;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import iq.earthlink.social.postservice.post.rest.JsonComment;
import iq.earthlink.social.postservice.post.rest.JsonPost;
import iq.earthlink.social.postservice.post.rest.JsonVoteCount;
import iq.earthlink.social.postservice.post.vote.CommentVote;
import iq.earthlink.social.postservice.post.vote.VoteCount;
import iq.earthlink.social.postservice.post.vote.repository.CommentVoteRepository;
import iq.earthlink.social.postservice.util.CommentUtil;
import iq.earthlink.social.postservice.util.PermissionUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultCommentService implements CommentService {

    private static final Logger LOGGER = LogManager.getLogger(DefaultCommentService.class);
    private static final String ERROR_NOT_FOUND_POST = "error.not.found.post";
    private static final String ERROR_NOT_FOUND_COMMENT = "error.not.found.comment";

    private final RabbitTemplate rabbitTemplate;
    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final PostRepository postRepository;
    private final PersonBanRestService personBanRestService;
    private final CommentComplaintRepository commentComplaintRepository;
    private final PermissionUtil permissionUtil;
    private final KafkaProducerService kafkaProducerService;
    private final PostNotificationSettingsRepository postNotificationSettingsRepository;
    private final CommentMediaService commentMediaService;
    private final PersonRestService personRestService;
    private final GroupMemberManager groupMemberManager;
    private final GroupManager groupManager;
    private final PersonManager personManager;
    private final CommentUtil commentUtil;
    private final Mapper mapper;

    @Value("${social.postservice.comments.number-of-replies-to-show}")
    private int NUMBER_OF_REPLIES;

    @Value("${social.postservice.comments.top-replies-depth}")
    private int TOP_REPLIES_DEPTH;

    @Transactional
    @Nonnull
    @Override
    public JsonComment createComment(String authorizationHeader, @Nonnull PersonDTO author, @Nonnull CommentData commentData) {
        validateCommentData(commentData);
        Post post = postRepository.findByPostUuid(commentData.getPostUuid())
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_POST, commentData.getPostUuid()));

        checkIfCanCreateComment(authorizationHeader, author, post);

        Comment comment = new Comment();
        comment.setContent(commentData.getContent());
        comment.setAuthorId(author.getPersonId());
        comment.setAuthorUuid(author.getUuid());
        comment.setPost(post);
        comment = commentRepository.saveAndFlush(comment);

        PostActivityEvent.send(rabbitTemplate, Map.of(PostActivityEvent.POST_ID, post.getId(),
                PostActivityEvent.POST_EVENT_TYPE, PostEventType.POST_COMMENT_ADDED.name()));

        String key = post.getUserGroupId() + "_" + post.getAuthorId();
        JsonPost jsonPost = mapper.map(post, JsonPost.class);
        kafkaProducerService.sendMessage(String.valueOf(KafkaTopics.POST_COMMENTED), key, jsonPost);

        GroupDTO group = groupManager.getGroupById(post.getUserGroupId());
        PersonData eventAuthor = PersonData
                .builder()
                .id(author.getPersonId())
                .avatar(author.getAvatar())
                .displayName(author.getDisplayName())
                .build();
        Set<Long> mentionedIds = new HashSet<>();
        if (CollectionUtils.isNotEmpty(commentData.getMentionedPersonIds())) {
            mentionedIds = getNotificationRecipients(post, comment, null, commentData, NotificationType.PERSON_IS_MENTIONED_IN_COMMENT);
            sendNotification(eventAuthor, group, post, comment, null, NotificationType.PERSON_IS_MENTIONED_IN_COMMENT, commentData, new HashMap<>(), mentionedIds);
        }

        Set<Long> receiverIds = getNotificationRecipients(post, comment, null, commentData, NotificationType.POST_COMMENTED);
        receiverIds.removeAll(mentionedIds);
        sendNotification(eventAuthor, group, post, comment, null, NotificationType.POST_COMMENTED, commentData, new HashMap<>(), receiverIds);

        receiverIds = getNotificationRecipients(post, comment, null, commentData, NotificationType.POST_COMMENTED_EXT);
        receiverIds.removeAll(mentionedIds);
        sendNotification(eventAuthor, group, post, comment, null, NotificationType.POST_COMMENTED_EXT, commentData, new HashMap<>(), receiverIds);

        return enrichComments(author.getPersonId(), Collections.singletonList(comment), 0).get(0);
    }

    @Transactional
    @Nonnull
    @Override
    public JsonComment createCommentWithFile(String authorizationHeader, @Nonnull PersonDTO author,
                                             @Nonnull CommentData commentData, MultipartFile file) {

        validateCommentData(commentData, file);
        JsonComment jsonComment = createComment(authorizationHeader, author, commentData);

        if (file != null) {
            commentMediaService.uploadCommentFile(jsonComment.getId(), file);
            enrichWithMedia(jsonComment);
        }
        return jsonComment;
    }

    @NonNull
    @Override
    public JsonComment getCommentWithReplies(Long personId, @NonNull UUID commentUuid) {
        Comment comment = getCommentByUuidInternal(commentUuid);

        if (Objects.equals(comment.getAuthorId(), personId) || !Objects.isNull(groupMemberManager.getGroupMember(comment.getPost().getUserGroupId(), personId))) {
            List<Comment> replies = commentRepository.findReplies(comment.getId(), false, PageRequest.of(0, NUMBER_OF_REPLIES)).getContent();
            if (CollectionUtils.isNotEmpty(replies)) {
                comment.setReplies(replies);
            }
            return enrichComments(personId, Collections.singletonList(comment), 0).get(0);
        } else {
            throw new NotFoundException(ERROR_NOT_FOUND_COMMENT, commentUuid.toString());
        }
    }

    @NonNull
    @Override
    public Comment getCommentByUuidInternal(@NonNull UUID commentUuid) {
        return commentRepository.findByCommentUuid(commentUuid)
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_COMMENT, commentUuid.toString()));
    }

    @Nonnull
    @Override
    public Page<JsonComment> findComments(@Nonnull Long personId, @Nonnull Boolean isAdmin, @Nonnull UUID postUuid, Boolean showAll, @Nonnull Pageable page) {
        Post post = getPostByUuid(postUuid);

        // Check if user has access to this group:
        if (!groupManager.hasAccessToGroup(personId, isAdmin, post.getUserGroupId())) {
            return Page.empty(page);
        }

        Page<Comment> comments = commentRepository.findComments(post.getId(), showAll, page);
        // Add configured number of replies to each comment:
        comments.forEach(c -> {
            List<Comment> replies = commentRepository.findReplies(c.getId(), showAll, PageRequest.of(0, NUMBER_OF_REPLIES)).getContent();
            c.setReplies(replies);
        });
        List<JsonComment> jsonComments = enrichComments(personId, comments.getContent(), 0);

        return new PageImpl<>(jsonComments, comments.getPageable(), comments.getTotalElements());
    }

    public List<JsonComment> getCommentsInternal(@Nonnull Long postId) {
        Page<Comment> comments = commentRepository.findComments(postId, false, PageRequest.of(0, 10));
        // Add configured number of replies to each comment:
        comments.forEach(c -> {
            List<Comment> replies = commentRepository.findReplies(c.getId(), false, PageRequest.of(0, NUMBER_OF_REPLIES)).getContent();
            c.setReplies(replies);
        });
        return enrichComments(null, comments.getContent(), 0);
    }

    @Nonnull
    @Override
    public Page<JsonComment> findCommentsWithComplaints(@Nonnull PersonDTO person, Long groupId,
                                                        CommentComplaintState complaintState, boolean deleted, @Nonnull Pageable page) {

        List<Long> groupIds = new ArrayList<>();
        if (Objects.isNull(groupId)) {
            groupIds = permissionUtil.getModeratedGroups(person.getPersonId());
        } else {
            permissionUtil.checkGroupPermissions(person, groupId);
            groupIds.add(groupId);
        }

        Page<Comment> comments = commentComplaintRepository.findCommentsWithComplaints(groupIds, complaintState, deleted, page);
        List<JsonComment> jsonComments = enrichComments(person.getPersonId(), comments.getContent(), 0);
        return new PageImpl<>(jsonComments, comments.getPageable(), comments.getTotalElements());
    }

    @Transactional
    @Nonnull
    @Override
    public JsonComment reply(
            @Nonnull String authorizationHeader,
            @Nonnull PersonDTO replyAuthor,
            @Nonnull UUID sourceCommentUuid,
            @Nonnull CommentData data,
            MultipartFile file) {

        validateCommentData(data);

        Comment sourceComment = getCommentByUuidInternal(sourceCommentUuid);
        Post post = sourceComment.getPost();

        checkIfCanCreateComment(authorizationHeader, replyAuthor, post);

        Comment reply = Comment.builder()
                .authorId(replyAuthor.getPersonId())
                .authorUuid(replyAuthor.getUuid())
                .content(data.getContent())
                .replyTo(sourceComment)
                .post(post)
                .build();

        reply = commentRepository.save(reply);
        updateCommentRepliesCount(sourceComment);
        PostActivityEvent.send(rabbitTemplate, Map.of(PostActivityEvent.POST_ID, post.getId(),
                PostActivityEvent.POST_EVENT_TYPE, PostEventType.POST_COMMENT_ADDED.name()));

        LOGGER.info("Sending post commented event: postId = {}, commentId = {}", post.getId(), reply.getId());

        GroupDTO group = groupManager.getGroupById(post.getUserGroupId());

        Set<Long> mentionedIds = new HashSet<>();
        PersonData eventAuthor = PersonData
                .builder()
                .id(replyAuthor.getPersonId())
                .displayName(replyAuthor.getDisplayName())
                .avatar(replyAuthor.getAvatar())
                .build();
        if (CollectionUtils.isNotEmpty(data.getMentionedPersonIds())) {
            mentionedIds = getNotificationRecipients(post, sourceComment, reply, data, NotificationType.PERSON_IS_MENTIONED_IN_COMMENT);
            sendNotification(eventAuthor, group, post, sourceComment, reply, NotificationType.PERSON_IS_MENTIONED_IN_COMMENT, data, new HashMap<>(), mentionedIds);
        }

        if (!isRecipientMuted(sourceComment.getAuthorId(), post.getId())) {
            Set<Long> receiverIds = getNotificationRecipients(post, sourceComment, reply, data, NotificationType.COMMENT_REPLIED);
            receiverIds.removeAll(mentionedIds);
            sendNotification(eventAuthor, group, post, sourceComment, reply, NotificationType.COMMENT_REPLIED, data, new HashMap<>(), receiverIds);
        }

        if (file != null) {
            commentMediaService.uploadCommentFile(reply.getId(), file);
        }

        return enrichComments(replyAuthor.getPersonId(), Collections.singletonList(reply), 0).get(0);
    }

    @Transactional
    @Nonnull
    @Override
    public JsonComment edit(
            String authorizationHeader, @Nonnull PersonDTO editor,
            @Nonnull UUID commentUuid,
            @Nonnull CommentData data,
            MultipartFile file) {

        validateCommentData(data);

        Comment comment = getCommentByUuidInternal(commentUuid);

        checkIfCanEditComment(authorizationHeader, editor, comment);

        comment.setContent(data.getContent());
        comment.setModifiedBy(editor.getPersonId());

        Comment saved = commentRepository.save(comment);

        String logMessage = String.format("Comment: \"%s\" was edited by \"%s\"", saved.getId(), editor.getPersonId());
        String auditMessage = AuditMessage.create(EventAction.EDIT_POST_COMMENT, logMessage, editor.getPersonId(), saved.getId());
        LOGGER.info(AuditMarker.getMarker(), auditMessage);

        if (file != null) {
            commentMediaService.uploadCommentFile(saved.getId(), file);
        }

        return enrichComments(editor.getPersonId(), Collections.singletonList(saved), 0).get(0);
    }

    @Transactional
    @Nonnull
    @Override
    public JsonComment editWithFile(
            String authorizationHeader,
            @Nonnull PersonDTO editor,
            @Nonnull UUID commentUuid,
            @Nonnull CommentData data,
            MultipartFile file) {

        validateCommentData(data, file);

        return edit(authorizationHeader, editor, commentUuid, data, file);
    }

    @Transactional
    @Override
    public void rejectCommentByComplaint(
            @Nonnull PersonDTO requester,
            @Nonnull String reason,
            @Nonnull UUID complaintUuid) {

        CommentComplaint commentComplaint = commentComplaintRepository
                .findByComplaintUuid(complaintUuid)
                .orElseThrow(() -> new NotFoundException("error.not.found.complaint", complaintUuid));

        permissionUtil.checkGroupPermissions(requester, commentComplaint.getComment().getPost().getUserGroupId());

        if (!CommentComplaintState.PENDING.equals(commentComplaint.getState())) {
            throw new BadRequestException("error.comment.complaint.already.resolved", complaintUuid, commentComplaint.getComment().getId());
        }
        // Reject comment - mark as deleted:
        Long commentId = commentComplaint.getComment().getId();
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_COMMENT, commentId));
        reject(comment);

        commentComplaintRepository.resolvePendingCommentComplaints(commentId, requester.getPersonId(), reason);


        // Send notification to comment author:
        Long postId = commentComplaint.getComment().getPost().getId();
        Post post = postRepository.findById(postId).orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_POST, postId));
        GroupDTO group = groupManager.getGroupById(post.getUserGroupId());
        PersonData eventAuthor = PersonData
                .builder()
                .id(requester.getPersonId())
                .displayName(requester.getDisplayName())
                .avatar(requester.getAvatar())
                .build();
        sendNotification(eventAuthor, group, post, comment, NotificationType.COMMENT_DELETED_BY_MODERATOR, null, new HashMap<>());

    }

    @Transactional
    @Override
    public void removeComment(PersonDTO requester, @Nonnull UUID commentUuid) {
        Comment comment = getCommentByUuidInternal(commentUuid);
        if (requester == null || canChange(requester, comment)) {
            remove(comment);
        } else {
            throw new ForbiddenException("error.person.can.not.delete.comment");
        }
        String logMessage;
        if (requester == null) {
            logMessage = String.format("Comment: \"%s\" was removed by content moderator", comment.getId());
            LOGGER.info(logMessage);
            String auditMessage = AuditMessage.create(EventAction.DELETE_POST_COMMENT, logMessage, 0L, comment.getId());
            LOGGER.info(AuditMarker.getMarker(), auditMessage);
        } else {
            logMessage = String.format("Comment: \"%s\" was removed by \"%s\"", comment.getId(), requester.getPersonId());
            String auditMessage = AuditMessage.create(EventAction.DELETE_POST_COMMENT, logMessage, requester.getPersonId(), comment.getId());
            LOGGER.info(AuditMarker.getMarker(), auditMessage);
        }
    }

    @Nonnull
    @Override
    public Page<JsonComment> getReplies(@Nonnull Long personId, @Nonnull Boolean isAdmin, UUID commentUuid, Boolean showAll, Pageable page) {
        if (commentUuid == null) {
            return Page.empty(page);
        }
        Comment comment = getCommentByUuidInternal(commentUuid);
        // Check if user has access to this group:
        if (!groupManager.hasAccessToGroup(personId, isAdmin, comment.getPost().getUserGroupId())) {
            throw new NotFoundException(ERROR_NOT_FOUND_COMMENT, commentUuid.toString());
        }
        Page<Comment> replies = commentRepository.findReplies(comment.getId(), showAll, page);
        List<JsonComment> jsonReplies = enrichComments(personId, replies.getContent(), 0);
        return new PageImpl<>(jsonReplies, replies.getPageable(), replies.getTotalElements());
    }

    @Override
    @Transactional
    public void removeCommentByModerator(ContentModerationDto dto) {
        Comment comment = getComment(dto.getId());

        Post post = postRepository.findById(comment.getPost().getId())
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_POST, comment.getPost().getId()));

        remove(comment);

        LOGGER.info("Removed post comment due to content moderation: postId = {}, commentId = {}",
                comment.getPost().getId(), dto.getId());

        GroupDTO group = groupManager.getGroupById(post.getUserGroupId());

        sendNotification(null, group, post, comment, NotificationType.COMMENT_DELETED_BY_MODERATOR, null, new HashMap<>());
    }

    @Override
    @Transactional
    public void removePostComments(PersonDTO person, Long postId) {
        // Find root comments:
        List<Comment> comments = commentRepository.findComments(postId, true, Pageable.unpaged()).getContent();
        if (CollectionUtils.isNotEmpty(comments)) {
            if (person == null) {
                comments.forEach(comment -> removeComment(null, comment.getCommentUuid()));
            } else {
                comments.forEach(this::remove);
            }
        }
    }

    @Override
    public void sendNotification(PersonData person, @Nonnull GroupDTO group, @Nonnull Post post,
                                 @Nonnull Comment comment, @Nonnull NotificationType type, CommentData commentData,
                                 Map<String, String> additionalMetadata) {

        Set<Long> receiverIds = getNotificationRecipients(post, comment, null, commentData, type);

        sendNotification(person, group, post, comment, null, type, commentData, additionalMetadata, receiverIds);
    }

    @Override
    public void sendNotification(PersonData person, @Nonnull GroupDTO group, @Nonnull Post post,
                                 @Nonnull Comment comment, Comment reply, @Nonnull NotificationType type, CommentData commentData,
                                 Map<String, String> additionalMetadata, Set<Long> receiverIds) {

        if (CollectionUtils.isNotEmpty(receiverIds)) {
            // Ensure that event author won't get notification:
            if (person != null) {
                receiverIds.remove(person.getId());
            }

            NotificationEvent event = NotificationEvent
                    .builder()
                    .receiverIds(new ArrayList<>(receiverIds))
                    .type(type)
                    .state(NotificationState.NEW)
                    .metadata(getMetadata(group, post, comment, reply, additionalMetadata)).build();

            if (person != null) {
                event.setEventAuthor(person);
            }
            // Send event for notification:
            kafkaProducerService.sendMessage("PUSH_NOTIFICATION", event);
        }
    }

    @Override
    @Transactional
    public void deleteMediaFile(String authorizationHeader, PersonDTO person, UUID commentUuid) {
        Comment comment = getCommentByUuidInternal(commentUuid);

        checkIfCanEditComment(authorizationHeader, person, comment);

        commentMediaService.removeCommentFile(comment.getId());
    }

    private void enrichWithMedia(JsonComment jsonComment) {
        JsonMediaFile file = commentMediaService.findCommentFileWithFullPath(jsonComment.getId());
        jsonComment.setFile(file);
    }

    private List<JsonComment> enrichComments(Long personId, List<Comment> comments, int level) {
        List<JsonComment> jsonComments = new ArrayList<>();
        List<Long> pIds = comments.stream().map(Comment::getId).toList();

        List<VoteCount> voteCounts = commentUtil.getCommentVoteCounts(personId, pIds);

        Map<Long, VoteCount> commentVoteCountMap = voteCounts.stream().collect(Collectors.toMap(VoteCount::getId, Function.identity()));

        for (Comment comment : comments) {
            VoteCount voteCount = commentVoteCountMap.get(comment.getId());
            jsonComments.add(enrichCommentWithTopRepliesAndDetails(personId, comment, voteCount, level));
        }
        return jsonComments;
    }

    @Nonnull
    private Comment getComment(@Nonnull Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_COMMENT, commentId));
    }

    private Map<String, String> getMetadata(@Nonnull GroupDTO group, @Nonnull Post post,
                                            @Nonnull Comment comment, Comment reply, Map<String, String> additionalMetadata) {
        String shortContent = CommonUtil.getPartialString(reply != null ? reply.getContent() : comment.getContent());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("routeTo", ContentType.POST_COMMENT.name());
        metadata.put("commentId", comment.getId().toString());
        metadata.put("commentUuid", comment.getCommentUuid().toString());
        metadata.put("postId", post.getId().toString());
        metadata.put("postUuid", post.getPostUuid().toString());
        metadata.put("commentText", shortContent);
        metadata.put("groupName", group.getName());
        metadata.put("groupId", group.getGroupId().toString());
        if (reply != null) {
            metadata.put("replyId", reply.getId().toString());
            metadata.put("replyUuid", reply.getCommentUuid().toString());
        }
        metadata.putAll(additionalMetadata);

        return metadata;
    }

    private Set<Long> getNotificationRecipients(Post post, Comment comment, Comment reply, CommentData commentData, NotificationType type) {
        Set<Long> recipients = new HashSet<>();
        Long postAuthorId = post.getAuthorId();
        Long authorId = reply == null ? comment.getAuthorId() : reply.getAuthorId();
        switch (type) {
            case POST_COMMENTED:
                // Send notification to the post author if post commented by other person:
                if (!postAuthorId.equals(authorId)) {
                    recipients.add(postAuthorId);
                }
                break;
            case POST_COMMENTED_EXT:
                List<Comment> postComments = commentRepository.findComments(post.getId(), false, Pageable.unpaged()).getContent();
                recipients.addAll(filterNotMutedRecipients(postComments.stream().map(Comment::getAuthorId).collect(Collectors.toSet()), post.getId()));
                recipients.remove(comment.getAuthorId());
                recipients.remove(postAuthorId);
                break;
            case COMMENT_VOTE_ADDED, COMMENT_DELETED_BY_MODERATOR, COMMENT_REPLIED:
                recipients.add(comment.getAuthorId());
                break;
            case PERSON_IS_MENTIONED_IN_COMMENT:
                recipients.addAll(filterNotMutedRecipients(new HashSet<>(commentData.getMentionedPersonIds()), post.getId()));
                List<Long> personIdsWhoMutedFollowingId = personRestService.getPersonIdsWhoMutedFollowingId();
                personIdsWhoMutedFollowingId.forEach(recipients::remove);
                recipients.remove(postAuthorId);
                break;
            default:
                break;
        }
        return recipients;
    }

    private Set<Long> filterNotMutedRecipients(Set<Long> recipients, Long postId) {
        return recipients.stream().filter(r -> !isRecipientMuted(r, postId)).collect(Collectors.toSet());
    }


    private Post getPostByUuid(UUID postUuid) {
        return postRepository.findByPostUuid(postUuid)
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_POST, postUuid));
    }

    private boolean isRecipientMuted(Long personId, Long postId) {
        Optional<PostNotificationSettings> settings = postNotificationSettingsRepository.findByPersonIdAndPostId(personId, postId);
        return settings.isPresent() && settings.get().isMuted();
    }

    private void updateCommentRepliesCount(Comment sourceComment) {
        long totalReplies = commentRepository.findReplies(sourceComment.getId(), false, Pageable.unpaged()).getTotalElements();
        sourceComment.setReplyCommentsCount(totalReplies);
        commentRepository.save(sourceComment);
    }

    private void remove(Comment comment) {
        List<Comment> replies = findAllReplies(comment, new ArrayList<>());

        List<Long> relatedCommentIds = replies.stream().map(Comment::getId).collect(Collectors.toList());
        relatedCommentIds.add(comment.getId());

        List<CommentVote> commentVotes = commentVoteRepository.getCommentVotes(relatedCommentIds);
        commentVoteRepository.deleteAll(commentVotes);

        commentMediaService.removeCommentFile(comment.getId());

        commentComplaintRepository.deleteByCommentIdIn(relatedCommentIds);
        commentRepository.deleteAll(replies);
        commentRepository.delete(comment);

        // Remove this comment from parent comment replies (if any):
        if (Objects.nonNull(comment.getReplyTo())) {
            Optional<Comment> parent = commentRepository.findById(comment.getReplyTo().getId());
            parent.ifPresent(value -> value.getReplies().remove(comment));
        }

        updateCommentStatistics(comment, commentVotes);
    }

    private void updateCommentStatistics(Comment comment, List<CommentVote> commentVotes) {
        for (CommentVote commentVote : commentVotes) {
            String eventType = VoteType.UPVOTE.equals(VoteType.getVoteType(commentVote.getVoteType()))
                    ? PostEventType.POST_COMMENT_UPVOTE_REMOVED.name()
                    : PostEventType.POST_COMMENT_DOWNVOTE_REMOVED.name();
            PostActivityEvent.send(rabbitTemplate,
                    Map.of(PostActivityEvent.POST_ID, commentVote.getId().getComment().getPost().getId(),
                            PostActivityEvent.POST_EVENT_TYPE, eventType));
        }

        PostActivityEvent.send(rabbitTemplate, Map.of(PostActivityEvent.POST_ID, comment.getPost().getId(),
                PostActivityEvent.POST_EVENT_TYPE, PostEventType.POST_COMMENT_REMOVED.name()));

        if (Objects.nonNull(comment.getReplyTo())) {
            updateCommentRepliesCount(comment.getReplyTo());
        }
    }

    private List<Comment> findAllReplies(Comment comment, List<Comment> comments) {
        List<Comment> replies = commentRepository.findReplies(comment.getId(), true, Pageable.unpaged()).getContent();
        comments.addAll(replies);
        replies.forEach(r -> findAllReplies(r, comments));
        return comments;
    }

    private boolean canChange(PersonDTO requester, Comment comment) {
        return Objects.equals(requester.getPersonId(), comment.getAuthorId())
                || permissionUtil.hasGroupPermissions(requester, comment.getPost().getUserGroupId());
    }

    private boolean canComment(PersonDTO author, Post post) {
        return Objects.equals(author.getPersonId(), post.getAuthorId()) ||
                author.isAdmin() || permissionUtil.isGroupMember(author, post.getUserGroupId());
    }

    private void checkIfCanCreateComment(String authorizationHeader, @NonNull PersonDTO author, Post post) {
        if (!PostState.PUBLISHED.equals(post.getState())) {
            throw new ForbiddenException("error.invalid.post.state.to.add.comment");
        }

        if (!post.isCommentsAllowed()) {
            throw new ForbiddenException("error.comments.not.allowed.for.post", post.getId());
        }

        if (!canComment(author, post)) {
            throw new ForbiddenException("error.person.can.not.comment.post");
        }

        if (personBanRestService.isPersonBannedFromGroup(authorizationHeader,
                post.getUserGroupId(), author.getPersonId())) {
            throw new ForbiddenException("error.add.comment.user.banned");
        }
    }

    private void checkIfCanEditComment(String authorizationHeader, @NonNull PersonDTO editor, Comment comment) {
        if (!canChange(editor, comment)) {
            throw new ForbiddenException("error.person.can.not.modify.comment");
        }

        if (personBanRestService.isPersonBannedFromGroup(authorizationHeader,
                comment.getPost().getUserGroupId(), editor.getPersonId())) {
            throw new ForbiddenException("error.edit.comment.user.banned");
        }
    }

    private void reject(Comment comment) {
        comment.setDeleted(true);
        comment.setDeletedAt(new Date(System.currentTimeMillis()));
        commentRepository.save(comment);

        List<CommentVote> commentVotes = commentVoteRepository.getCommentVotes(Collections.singletonList(comment.getId()));
        commentVoteRepository.deleteAll(commentVotes);
        updateCommentStatistics(comment, commentVotes);
    }

    private void validateCommentData(CommentData data) {
        if (!data.isAllowEmptyContent()) {
            validateCommentData(data, null);
        }
    }

    private void validateCommentData(CommentData data, MultipartFile file) {
        if (StringUtils.isBlank(data.getContent().trim()) && file == null) {
            throw new BadRequestException("error.comment.content.is.blank");
        }
    }

    private JsonComment enrichCommentWithTopRepliesAndDetails(Long personId, Comment comment, VoteCount voteCount, int level) {

        JsonComment jsonComment = mapper.map(comment, JsonComment.class);
        CommentDetailStatistics detailStatistics = new CommentDetailStatistics();
        detailStatistics.setCommentId(comment.getId());
        detailStatistics.setReplyCommentsCount(comment.getReplyCommentsCount());

        if (Objects.nonNull(voteCount)) {
            JsonVoteCount jsVote = mapper.map(voteCount, JsonVoteCount.class);
            jsonComment.setTotalVotes(jsVote);
            detailStatistics.setUpvotesCount(jsVote.getUpvotesTotal());
            detailStatistics.setDownvotesCount(jsVote.getDownvotesTotal());
            detailStatistics.setVoteValue(jsVote.getVoteValue());
        }
        jsonComment.setStats(detailStatistics);

        enrichWithAuthorDetails(comment.getAuthorUuid(), jsonComment);
        enrichWithMedia(jsonComment);

        List<Comment> replies = comment.getReplies();
        if (CollectionUtils.isNotEmpty(replies) && (level < TOP_REPLIES_DEPTH)) {
            jsonComment.setTopReplies(enrichComments(personId, replies, ++level));
        }

        return jsonComment;
    }

    private void enrichWithAuthorDetails(UUID authorUuid, JsonComment jsonComment) {
        PersonDTO personDTO = personManager.getPersonByUuid(authorUuid);
        AuthorDetails authorDetails = AuthorDetails.builder()
                .id(personDTO.getPersonId())
                .uuid(authorUuid)
                .avatar(getAvatarDetails(personDTO.getAvatar()))
                .displayName(personDTO.getDisplayName())
                .isVerified(personDTO.isVerifiedAccount())
                .build();

        jsonComment.setAuthor(authorDetails);
    }

    public AvatarDetails getAvatarDetails(JsonMediaFile avatarMediaFile) {
        AvatarDetails avatarDetails = null;
        if (avatarMediaFile != null) {
            avatarDetails = AvatarDetails
                    .builder()
                    .mimeType(avatarMediaFile.getMimeType())
                    .path(avatarMediaFile.getPath())
                    .size(avatarMediaFile.getSize())
                    .sizedImages(avatarMediaFile.getSizedImages())
                    .build();
        }
        return avatarDetails;
    }
}
