package iq.earthlink.social.postservice.post;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import feign.FeignException;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import iq.earthlink.social.classes.data.dto.ContentModerationDto;
import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.*;
import iq.earthlink.social.common.audit.AuditMarker;
import iq.earthlink.social.common.audit.AuditMessage;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.filestorage.minio.MinioProperties;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.common.util.CommonUtil;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.common.util.FileUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.groupservice.group.rest.JsonGroupMemberWithNotificationSettings;
import iq.earthlink.social.personservice.rest.FollowingRestService;
import iq.earthlink.social.personservice.rest.PersonBanRestService;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.postservice.event.KafkaProducerService;
import iq.earthlink.social.postservice.group.AccessType;
import iq.earthlink.social.postservice.group.GroupManager;
import iq.earthlink.social.postservice.group.PostingPermission;
import iq.earthlink.social.postservice.group.dto.GroupDTO;
import iq.earthlink.social.postservice.group.member.GroupMemberManager;
import iq.earthlink.social.postservice.group.member.dto.GroupMemberDTO;
import iq.earthlink.social.postservice.group.member.enumeration.Permission;
import iq.earthlink.social.postservice.group.notificationsettings.GroupNotificationSettingsManager;
import iq.earthlink.social.postservice.person.PersonManager;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.collection.GroupPostCollection;
import iq.earthlink.social.postservice.post.collection.repository.GroupPostCollectionRepository;
import iq.earthlink.social.postservice.post.collection.repository.PostCollectionPostsRepository;
import iq.earthlink.social.postservice.post.comment.CommentService;
import iq.earthlink.social.postservice.post.complaint.model.PostComplaint;
import iq.earthlink.social.postservice.post.complaint.repository.PostComplaintRepository;
import iq.earthlink.social.postservice.post.dto.PostEventDTO;
import iq.earthlink.social.postservice.post.dto.PostEventType;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettings;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettingsRepository;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import iq.earthlink.social.postservice.post.rest.*;
import iq.earthlink.social.postservice.post.statistics.PostStatistics;
import iq.earthlink.social.postservice.post.statistics.PostStatisticsManager;
import iq.earthlink.social.postservice.post.vote.repository.PostVoteRepository;
import iq.earthlink.social.postservice.util.PermissionUtil;
import iq.earthlink.social.postservice.util.PostStatisticsAndMediaUtil;
import iq.earthlink.social.util.ExceptionUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static iq.earthlink.social.common.util.Preconditions.checkNotNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Service
@RequiredArgsConstructor
public class DefaultPostManager implements PostManager {

    private static final Logger LOGGER = LogManager.getLogger(DefaultPostManager.class);
    private static final String ERROR_NOT_FOUND_POST = "error.not.found.post";
    private static final String ERROR_CHECK_NOT_NULL = "error.check.not.null";
    private static final String ERROR_OPERATION_NOT_PERMITTED = "error.operation.not.permitted";
    private static final String ERROR_PERSON_CAN_NOT_CREATE_POST = "error.person.can.not.create.post";
    public static final String LINK_TYPE = "type";
    public static final String LINK_MEDIA_ID = "mediaId";
    public static final String LINK_TITLE = "title";
    public static final String LINK_DESCRIPTION = "description";
    public static final String LINK_THUMBNAIL = "thumbnail";
    public static final String LINK_URL = "url";

    private final WebClient webClient;

    @Value("${transcode.service.url}")
    private String transcodeApiUri;

    private final PostMediaService mediaService;
    private final PostStatisticsManager postStatisticsManager;
    private final CommentService commentService;
    private final PostRepository repository;
    private final PostComplaintRepository postComplaintRepository;
    private final PersonManager personManager;
    private final PersonBanRestService personBanRestService;
    private final FollowingRestService followingRestService;
    private final PersonRestService personRestService;
    private final MinioProperties minioProperties;
    private final PostVoteRepository postVoteRepository;
    private final GroupPostCollectionRepository groupPostCollectionRepository;
    private final Mapper mapper;
    private final PermissionUtil permissionUtil;
    private final PostStatisticsProperties properties;
    private final KafkaProducerService kafkaProducerService;
    private final CompositeMeterRegistry meterRegistry;
    private final PostNotificationSettingsRepository postNotificationSettingsRepository;
    private final PostCollectionPostsRepository postCollectionPostsRepository;
    private final PostStatisticsAndMediaUtil postStatisticsAndMediaUtil;
    private final GroupManager groupManager;
    private final GroupMemberManager groupMemberManager;
    private final GroupNotificationSettingsManager groupNotificationSettingsManager;
    private final RedisTemplate<String, Long> migrationFlag;

    @Nonnull
    @Override
    public JsonPost getPost(Long personId, boolean isAdmin, @Nonnull Long postId) {
        Post post = repository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_POST, postId));

        if (!groupManager.hasAccessToGroup(personId, isAdmin, post.getUserGroupId())) {
            throw new NotFoundException(ERROR_NOT_FOUND_POST, postId);
        }

        return postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(personId, mapper.map(post, JsonPost.class));
    }

    @Nonnull
    @Override
    public Post getPost(@Nonnull Long postId) {
        return repository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_POST, postId));
    }

    @NonNull
    @Override
    public Post getPostByUuid(@NonNull UUID postUuid) {
        return repository.findByPostUuid(postUuid)
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_POST, postUuid));
    }

    @Transactional
    @Nonnull
    @Override
    public JsonPost createPostDeprecated(String authorizationHeader,
                                         @Nonnull PersonDTO personDTO,
                                         @Nonnull PostData postData,
                                         @Nullable MultipartFile[] files) {
        var post = createPostInternal(authorizationHeader, personDTO, postData, files);
        return mapper.map(post, JsonPost.class);
    }

    //shouldn't be used by controller layer
    @Override
    public Post createPostInternal(String authorizationHeader,
                                   @Nonnull PersonDTO personDTO,
                                   @Nonnull PostData postData,
                                   @Nullable MultipartFile[] files) {
        checkNotNull(postData.getUserGroupId(), ERROR_CHECK_NOT_NULL, "userGroupId");

        // Check if user is allowed to create post in this group:
        //todo: we should have this details with post service
        if (personBanRestService.isPersonBannedFromGroup(authorizationHeader, postData.getUserGroupId(), personDTO.getPersonId())) {
            throw new ForbiddenException("error.add.post.user.banned");
        }

        var memberInfo = groupMemberManager.getGroupMember(postData.getUserGroupId(), personDTO.getPersonId());
        if (memberInfo == null) {
            throw new ForbiddenException(ERROR_PERSON_CAN_NOT_CREATE_POST);
        }

        validatePostData(postData, files);

        boolean isGroupAdminOrModerator = permissionUtil.isGroupAdminOrModerator(memberInfo);

        var userGroup = groupManager.getGroupById(postData.getUserGroupId());

        if (PostingPermission.ADMIN_ONLY.equals(userGroup.getPostingPermission()) && !isGroupAdminOrModerator) {
            throw new ForbiddenException(ERROR_PERSON_CAN_NOT_CREATE_POST);
        }

        // Find initial post state considering group posting permission:
        PostState state = getPostState(userGroup.getPostingPermission(), postData.getState(), isGroupAdminOrModerator);

        Map<String, String> linkMeta = convertLinkMetaToMap(postData.getLinkMeta());
        Post post = Post
                .builder()
                .content(postData.getContent())
                .userGroupId(postData.getUserGroupId())
                .authorId(personDTO.getPersonId())
                .authorUuid(personDTO.getUuid())
                .authorDisplayName(personDTO.getDisplayName())
                .commentsAllowed(postData.getCommentsAllowed() == null || postData.getCommentsAllowed())
                .state(state)
                .userGroupType(userGroup.getAccessType())
                .publishedAt(state == PostState.PUBLISHED ? new Date() : null)
                .mentionedPersonIds(postData.getMentionedPersonIds())
                .linkMeta(linkMeta)
                .build();

        handleRepostedPost(postData, post);
        post.setPostType(post.getRepostedFrom() != null ? PostType.REPOSTED : PostType.ORIGINAL);
        // save to get generated id
        post = repository.save(post);

        uploadMediaFiles(files, post);

        post = repository.save(post);
        postStatisticsManager.savePostStatistics(PostStatistics.builder().post(post).build());

        Post finalPost = post;
        CompletableFuture.runAsync(() -> {
            LOGGER.info("Running in async all integrations");
            if (PostState.PUBLISHED.equals(finalPost.getState())) {
                // New post with PUBLISHED state was created by admin user - send event:
                String messageKey = StringUtils.join("group_", finalPost.getUserGroupId(), "_author_" + finalPost.getAuthorId());
                kafkaProducerService.sendMessage(CommonConstants.POST_EVENT, messageKey, PostEventDTO
                        .builder()
                        .personId(personDTO.getPersonId())
                        .groupId(userGroup.getGroupId())
                        .postId(finalPost.getId())
                        .postUuid(finalPost.getPostUuid().toString())
                        .publishedAt(finalPost.getPublishedAt())
                        .eventType(PostEventType.POST_PUBLISHED)
                        .build());
                meterRegistry.counter("post.created.kafka.message").increment();
                sendPostNotifications(authorizationHeader, personDTO, userGroup, finalPost, postData.getMentionedPersonIds());
            }
        });

        LOGGER.info("Created new post: {}", post);
        return post;
    }

    private void handleRepostedPost(@NotNull PostData postData, Post post) {
        if (Objects.nonNull(postData.getRepostedFromId())) {
            Optional<Post> repostedPost = repository.findById(postData.getRepostedFromId());

            if (repostedPost.isPresent()) {
                if (PostState.PUBLISHED.equals(repostedPost.get().getState())) {
                    if (AccessType.PUBLIC.equals(repostedPost.get().getUserGroupType()) || postData.getUserGroupId().equals(repostedPost.get().getUserGroupId())) {
                        post.setRepostedFrom(repostedPost.get());
                    }
                } else {
                    throw new ForbiddenException(ERROR_PERSON_CAN_NOT_CREATE_POST);
                }
            }
        }
    }

    private void sendPostNotifications(String authorizationHeader, @NotNull PersonDTO personDTO, GroupDTO userGroup, Post post, List<Long> mentionedUserIds) {
        PersonData eventAuthor = PersonData
                .builder()
                .id(personDTO.getPersonId())
                .displayName(personDTO.getDisplayName())
                .avatar(personDTO.getAvatar())
                .build();
        // New post is published to a group -> send notifications to the following users:
        // - Group members (except post author) who didn't mute the group.
        sendNotification(authorizationHeader, eventAuthor, post, userGroup, NotificationType.POST_PUBLISHED_TO_GROUP, new HashMap<>(), false, mentionedUserIds);
        // - Users that follow this user but not members of the group, if the group is public.
        sendNotification(authorizationHeader, eventAuthor, post, userGroup, NotificationType.POST_PUBLISHED_TO_GROUP_EXT, new HashMap<>(), false, mentionedUserIds);
        // - Users who are mentioned in the post.
        sendNotification(authorizationHeader, eventAuthor, post, userGroup, NotificationType.PERSON_IS_MENTIONED_IN_POST, new HashMap<>(), false, mentionedUserIds);
    }

    @Nonnull
    @Override
    public Page<JsonPost> findPostsDeprecated(Long personId, @Nonnull PostSearchCriteria criteria, @Nonnull Pageable page) {
        criteria.setUserId(personId);
        return findPostsInternal(criteria, page).map(post -> {
            JsonPost jsonPost = mapper.map(post, JsonPost.class);
            return postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(personId, jsonPost);
        });
    }

    @Nonnull
    public Page<Post> findPostsInternal(@Nonnull PostSearchCriteria criteria, @Nonnull Pageable page) {
        Page<Post> posts;
        updateQuery(criteria);

        SortType sortType = criteria.getSortType() != null ? criteria.getSortType() : SortType.ALL;
        switch (sortType) {
            case TOP -> posts = repository.findTopPosts(criteria, page);
            case POPULAR -> posts = findTopPostsInTimeInterval(criteria, page, Integer.parseInt(properties.getActivityPopularDaysCount()));
            case TRENDING -> posts = findTopPostsInTimeInterval(criteria, page, Integer.parseInt(properties.getActivityTrendingDaysCount()));
            case NEWEST -> {
                page = PageRequest.of(page.getPageNumber(), page.getPageSize(), Sort.Direction.DESC, Post.PUBLISHED_AT, Post.CREATED_AT);
                posts = repository.findPosts(criteria, page);
            }
            default -> posts = repository.findPosts(criteria, page);
        }
        return posts;
    }

    @Transactional
    @Nonnull
    @Override
    public JsonPost updatePostDeprecated(
            String authorizationHeader,
            @Nonnull PersonDTO personDTO,
            @Nonnull Long postId,
            UpdatePostData data,
            MultipartFile[] files) {

        var updatedPost = update(authorizationHeader, personDTO, postId, data, files);

        JsonPost jsonPost = mapper.map(updatedPost, JsonPost.class);
        return postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(personDTO.getPersonId(), jsonPost);
    }

    // can not be used by controller layer
    @Transactional
    public Post update(String authorizationHeader,
                       @Nonnull PersonDTO person,
                       @Nonnull Long postId,
                       UpdatePostData data,
                       MultipartFile[] files) {
        final Post post = getPost(postId);
        boolean hasPermissions = validateUserPermission(authorizationHeader, person, post);

        if (!isAuthor(person, post) &&
                (data != null &&
                        (data.getCommentsAllowed() != null ||
                                data.getShouldPin() != null ||
                                data.getContent() != null ||
                                data.getLinkMeta() != null ||
                                data.getMentionedPersonIds() != null) ||
                        !ArrayUtils.isEmpty(files))) {
            throw new ForbiddenException("error.person.can.not.modify.post");
        }
        if (Objects.nonNull(data)) {
            updatePostData(authorizationHeader, post, data, person, hasPermissions);
        }
        uploadMediaFiles(files, post);

        post.setPostType(post.getRepostedFrom() != null ? PostType.REPOSTED : PostType.ORIGINAL);
        return repository.save(post);
    }

    private boolean validateUserPermission(String authorizationHeader, PersonDTO person, Post post) {
        // Check if user is allowed to update post in group:
        if (personBanRestService.isPersonBannedFromGroup(authorizationHeader, post.getUserGroupId(), person.getPersonId())) {
            throw new ForbiddenException("error.update.post.user.banned");
        }

        boolean hasPermissions = permissionUtil.hasGroupPermissions(person, post.getUserGroupId());

        if (!(isAuthor(person, post) || hasPermissions)) {
            throw new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED);
        }

        return hasPermissions;
    }

    private void updatePostData(String authorizationHeader, Post post, UpdatePostData data, PersonDTO person, boolean hasPermissions) {
        post.setContent(firstNonNull(data.getContent(), post.getContent()));
        post.setCommentsAllowed(firstNonNull(data.getCommentsAllowed(), post.isCommentsAllowed()));

        List<Long> oldMentionedPersonIds = new ArrayList<>();
        if (post.getMentionedPersonIds() != null) {
            oldMentionedPersonIds = new ArrayList<>(post.getMentionedPersonIds());
        }

        List<Long> newMentionedPersonIds = new ArrayList<>();
        if (data.getMentionedPersonIds() != null) {
            newMentionedPersonIds = new ArrayList<>(data.getMentionedPersonIds());
        }

        post.setMentionedPersonIds(newMentionedPersonIds);

        if (data.getShouldPin() != null) {
            post.setPinned(data.getShouldPin());
        }

        PostState oldState = post.getState();
        if (Objects.nonNull(data.getState())) {
            updatePostState(post, data.getState(), hasPermissions);
        }

        Map<String, String> linkMeta = convertLinkMetaToMap(data.getLinkMeta());
        if (linkMeta != null) {
            post.setLinkMeta(linkMeta);
        }

        var group = groupManager.getGroupById(post.getUserGroupId());
        List<Long> finalNewMentionedPersonIds = newMentionedPersonIds;
        List<Long> finalOldMentionedPersonIds = oldMentionedPersonIds;
        CompletableFuture.runAsync(() -> {
            if (!post.getState().equals(oldState)) {
                String logMessage;
                if (PostState.PUBLISHED.equals(post.getState())) {
                    sendUpdatePostStateNotifications(authorizationHeader, person, post, group, finalNewMentionedPersonIds);
                    logMessage = String.format("The post \"%s\" was approved by \"%s\"", post.getId(), person.getPersonId());
                    var auditMessage = AuditMessage.create(EventAction.APPROVE_POST, logMessage, person.getPersonId(), post.getId());
                    LOGGER.info(AuditMarker.getMarker(), auditMessage);
                } else if (PostState.REJECTED.equals(post.getState())) {
                    logMessage = String.format("The post \"%s\" was rejected by \"%s\"", post.getId(), person.getPersonId());
                    var auditMessage = AuditMessage.create(EventAction.REJECT_POST, logMessage, person.getPersonId(), post.getId());
                    LOGGER.info(AuditMarker.getMarker(), auditMessage);
                }
            } else if (PostState.PUBLISHED.equals(post.getState())) {
                PersonData eventAuthor = PersonData
                        .builder()
                        .id(person.getPersonId())
                        .avatar(person.getAvatar())
                        .displayName(person.getDisplayName())
                        .build();
                if (!finalNewMentionedPersonIds.isEmpty()) {
                    finalNewMentionedPersonIds.removeAll(finalOldMentionedPersonIds);
                    // - Mentioned users who were added after the post was published.
                    sendNotification(authorizationHeader, eventAuthor, post, group, NotificationType.PERSON_IS_MENTIONED_IN_POST, new HashMap<>(), false, finalNewMentionedPersonIds);
                }
            }
        });
    }

    private void updatePostState(Post post, PostState state, boolean hasPermissions) {
        if (state == PostState.WAITING_TO_BE_PUBLISHED || hasPermissions) {
            updatePostState(post, state);
        } else {
            throw new ForbiddenException("error.person.can.not.modify.post");
        }
    }

    private void uploadMediaFiles(MultipartFile[] files, Post post) {
        if (Objects.nonNull(files) && files.length > 0) {
            List<MediaFile> mediaFiles = mediaService.uploadPostFiles(post, files);
            sendVideosForTranscoding(post.getId(), mediaFiles);
        }
    }

    @Transactional
    @Override
    public void removePost(@Nonnull PersonDTO person, @Nonnull Long postId) {
        final Post post = getPost(postId);

        if (isAuthor(person, post) || permissionUtil.hasGroupPermissions(person, post.getUserGroupId())) {
            removePostDependencies(person, post);
            repository.delete(post);
            LOGGER.info("Deleted post: {} by person: {}", post, person.getPersonId());
            String messageKey = StringUtils.join("group_", post.getUserGroupId(), "_author_" + post.getAuthorId());
            kafkaProducerService.sendMessage(CommonConstants.POST_EVENT, messageKey, PostEventDTO
                            .builder()
                            .personId(person.getPersonId())
                            .groupId(post.getUserGroupId())
                            .postId(post.getId())
                            .eventType(PostEventType.POST_UNPUBLISHED)
                            .build());
        } else {
            throw new ForbiddenException("error.person.can.not.delete.post");
        }
    }

    @Override
    public void removePostFile(@NotNull PersonDTO person, @NotNull Long postId, @NotNull Long fileId) {
        var post = getPost(postId);
        if ((isAuthor(person, post) || permissionUtil.hasGroupPermissions(person, post.getUserGroupId()))
                && StringUtils.isNotEmpty(post.getContent())) {
            mediaService.removePostFile(postId, fileId);
            LOGGER.info("Deleted post media file in post: {} by person: {}", post, person.getPersonId());
        } else {
            throw new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED);
        }
    }

    @Transactional
    @Override
    public void removeLinkMeta(@Nonnull PersonDTO person, @Nonnull Long postId) {
        Post post = getPost(postId);

        if ((isAuthor(person, post) || permissionUtil.hasGroupPermissions(person, post.getUserGroupId()))
                && StringUtils.isNotEmpty(post.getContent())) {
            post.setLinkMeta(null);
            repository.save(post);
            LOGGER.info("Deleted post LinkMeta in post: {} by person: {}", post, person.getPersonId());
        } else {
            throw new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED);
        }
    }

    @Override
    @Transactional
    public Page<JsonPost> findPostsWithComplaintsDeprecated(String authorizationHeader, PersonDTO person, Long groupId,
                                                            PostComplaintState complaintState, PostState postState, Pageable page) {
        checkNotNull(complaintState, ERROR_CHECK_NOT_NULL, "complaintState");
        checkNotNull(person, ERROR_CHECK_NOT_NULL, "person");

        var posts = findPostsWithComplaintsInternal(authorizationHeader, person, groupId, complaintState, postState, page);
        return posts.map(post -> {
            JsonPost jsonPost = mapper.map(post, JsonPost.class);
            return postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(person.getPersonId(), jsonPost);
        });
    }


    public Page<Post> findPostsWithComplaintsInternal(String authorizationHeader, PersonDTO person, Long groupId, PostComplaintState complaintState, PostState postState, Pageable page) {
        List<Long> groupIds = new ArrayList<>();

        if (Objects.isNull(groupId)) {
            groupIds = permissionUtil.getModeratedGroups(person.getPersonId());
        } else {
            permissionUtil.checkGroupPermissions(person, groupId);
            groupIds.add(groupId);
        }
        return postComplaintRepository.findPostsWithComplaints(groupIds, complaintState, postState, page);
    }

    @Transactional
    @Nonnull
    @Override
    public Post rejectPostByComplaintDeprecated(String authorizationHeader, PersonDTO person, String reason, Long complaintId) {

        PostComplaint complaint = postComplaintRepository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("error.not.found.complaint", complaintId));

        if (complaint.getState() != PostComplaintState.PENDING) {
            throw new BadRequestException("error.post.complaint.already.resolved", complaint.getId(), complaint.getState());
        }

        Post post = complaint.getPost();

        permissionUtil.checkGroupPermissions(person, post.getUserGroupId());

        // Update post state:
        post.setState(PostState.REJECTED);
        post.setStateChangedDate(new Date());
        repository.save(post);

        // Update post complaint:
        postComplaintRepository.resolvePendingPostComplaints(post.getId(), person.getPersonId(), reason);
        String messageKey = StringUtils.join("group_", post.getUserGroupId(), "_author_" + post.getAuthorId());
        kafkaProducerService.sendMessage(CommonConstants.POST_EVENT, messageKey, PostEventDTO
                .builder()
                .personId(post.getAuthorId())
                .groupId(post.getUserGroupId())
                .postId(post.getId())
                .eventType(PostEventType.POST_UNPUBLISHED)
                .build());
        var group = groupManager.getGroupById(post.getUserGroupId());
        PersonData eventAuthor = PersonData
                .builder()
                .id(person.getPersonId())
                .displayName(person.getDisplayName())
                .avatar(person.getAvatar())
                .build();
        sendNotification(authorizationHeader, eventAuthor, post, group, NotificationType.POST_DELETED_BY_MODERATOR, new HashMap<>(), false);

        return post;
    }

    @Transactional
    public void updatePostsAuthorDisplayName(Long authorId, String displayName) {
        LOGGER.info("Updating post author displayName: {} where authorId is {} ", displayName, authorId);
        if (Objects.nonNull(authorId) && Objects.nonNull(displayName)) {
            try {
                repository.updatePostAuthorDisplayName(displayName, authorId);
            } catch (Exception ex) {
                LOGGER.error("Failed to update post author displayName, reason: {}", ex.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void updatePostsUserGroupType(Long userGroupId, AccessType accessType) {
        LOGGER.info("Updating user group type to '{}' for all posts in group with ID {}", accessType, userGroupId);
        checkNotNull(userGroupId, ERROR_CHECK_NOT_NULL, "userGroupId");
        checkNotNull(accessType, ERROR_CHECK_NOT_NULL, "accessType");
        try {
            repository.updatePostUserGroupType(userGroupId, accessType);
        } catch (Exception ex) {
            LOGGER.error("Failed to update post user group type, reason: {}", ex.getMessage());
        }
    }

    @Transactional
    @Override
    public void updatePostGroupType(Long userGroupId, AccessType userGroupType) {
        try {
            repository.updatePostGroupTypeByGroupId(userGroupType, userGroupId);
        } catch (Exception ex) {
            LOGGER.error("Failed to update post group type: {}", ex.getMessage());
        }
    }

    @Override
    public PostStats getPostStats(String fromDate, TimeInterval timeInterval) {
        if (timeInterval == null) {
            timeInterval = TimeInterval.MONTH;
        }

        Timestamp timestamp = StringUtils.isEmpty(fromDate) ? null : Timestamp.valueOf(DateUtil.getDateFromString(fromDate).atStartOfDay());
        long allPostsCount = repository.getAllPostsCount();
        long newPostsCount = repository.getNewPostsCount(timestamp);

        PostStats stats = PostStats.builder()
                .allPostsCount(allPostsCount)
                .newPostsCount(newPostsCount)
                .fromDate(timestamp)
                .timeInterval(timeInterval)
                .build();

        switch (timeInterval) {
            case DAY -> {
                List<CreatedPosts> createdPostsPerDay = repository.getCreatedPostsPerDay(timestamp);
                stats.setCreatedPosts(createdPostsPerDay);
            }
            case YEAR -> {
                List<CreatedPosts> createdPostsPerYear = repository.getCreatedPostsPerYear(timestamp);
                stats.setCreatedPosts(createdPostsPerYear);
            }
            default -> {
                List<CreatedPosts> createdPostsPerMonth = repository.getCreatedPostsPerMonth(timestamp);
                stats.setCreatedPosts(createdPostsPerMonth);
            }
        }
        return stats;
    }

    @Override
    @Transactional
    public void removePostByModerator(@Nonnull ContentModerationDto dto) {
        Post post = getPost(dto.getId());
        removePostDependencies(null, post);
        repository.delete(post);
        LOGGER.info("Deleted post: {} due to content moderation", post);

        String messageKey = StringUtils.join("group_", post.getUserGroupId(), "_author_" + post.getAuthorId());
        kafkaProducerService.sendMessage(CommonConstants.POST_EVENT, messageKey, PostEventDTO
                .builder()
                .personId(post.getAuthorId())
                .groupId(post.getUserGroupId())
                .postId(post.getId())
                .eventType(PostEventType.POST_UNPUBLISHED)
                .build());
        var group = groupManager.getGroupById(post.getUserGroupId());

        sendNotification(null, null, post, group, NotificationType.POST_DELETED_BY_MODERATOR, new HashMap<>(), false);
    }

    @Override
    public void sendNotification(String authorizationHeader, PersonData eventAuthor, @Nonnull Post post, @Nonnull GroupDTO group,
                                 @Nonnull NotificationType type, @Nonnull Map<String, String> additionalMetadata,
                                 boolean sendToEventAuthor) {
        sendNotification(authorizationHeader, eventAuthor, post, group, type, additionalMetadata, sendToEventAuthor, Collections.emptyList());
    }

    @Override
    public void sendNotification(String authorizationHeader, PersonData eventAuthor, @Nonnull Post post, @Nonnull GroupDTO group,
                                 @Nonnull NotificationType type, @Nonnull Map<String, String> additionalMetadata,
                                 boolean sendToEventAuthor, List<Long> mentionedUserIds) {
        if (mentionedUserIds == null) {
            mentionedUserIds = new ArrayList<>();
        }
        Set<Long> receiverIds = getNotificationRecipients(authorizationHeader, group, post, type, mentionedUserIds);

        if (CollectionUtils.isNotEmpty(receiverIds)) {
            if (!sendToEventAuthor) {
                // Ensure that event author won't get notification:
                receiverIds.remove(eventAuthor.getId());
            }

            NotificationEvent event = NotificationEvent
                    .builder()
                    .receiverIds(new ArrayList<>(receiverIds))
                    .type(type)
                    .state(NotificationState.NEW)
                    .metadata(getMetadata(post, group, additionalMetadata)).build();

            if (eventAuthor != null) {
                event.setEventAuthor(eventAuthor);
            }
            // Send event for notification:
            kafkaProducerService.sendMessage("PUSH_NOTIFICATION", event);
        }
    }

    @Override
    public List<Long> getFrequentlyPostsGroups(Long personId) {
        return repository.findTopGroupIdsByPostFrequency(personId);
    }

    private Map<String, String> convertLinkMetaToMap(LinkMeta linkMeta) {
        Map<String, String> linkMetaMap = null;
        if (linkMeta != null && linkMeta.getType() != null
                && (linkMeta.getUrl() != null && !linkMeta.getUrl().isEmpty()
                || linkMeta.getMediaId() != null && !linkMeta.getMediaId().isEmpty())) {
            linkMetaMap = new HashMap<>();
            linkMetaMap.put(LINK_TYPE, linkMeta.getType() != null ? linkMeta.getType().name() : StringUtils.EMPTY);
            linkMetaMap.put(LINK_MEDIA_ID, linkMeta.getMediaId());
            linkMetaMap.put(LINK_TITLE, linkMeta.getTitle());
            linkMetaMap.put(LINK_DESCRIPTION, linkMeta.getDescription());
            linkMetaMap.put(LINK_THUMBNAIL, linkMeta.getThumbnail());
            linkMetaMap.put(LINK_URL, linkMeta.getUrl());
        }
        return linkMetaMap;
    }

    @NonNull
    private Map<String, String> getMetadata(@NonNull Post post,
                                            @NonNull GroupDTO group, Map<String, String> additionalMetadata) {
        String shortContent = CommonUtil.getPartialString(post.getContent());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("routeTo", ContentType.POST.name());
        metadata.put("postUuid", post.getPostUuid().toString());
        metadata.put("postId", post.getId().toString());
        metadata.put("postText", shortContent);
        metadata.put("groupName", group.getName());
        metadata.put("groupId", group.getGroupId().toString());
        metadata.putAll(additionalMetadata);

        return metadata;
    }

    private void sendVideosForTranscoding(Long postId, List<MediaFile> files) {
        if (CollectionUtils.isNotEmpty(files)) {
            for (MediaFile media : files) {
                try {
                    if (FileUtil.isVideo(media.getMimeType())) {
                        JsonTranscodeRequest json = new JsonTranscodeRequest();
                        json.setId(media.getPath());
                        json.setPostId(postId);
                        json.setObjectName(media.getPath());
                        json.setBucket(minioProperties.getBucketName());
                        webClient
                                .post()
                                .uri(transcodeApiUri + "/transcode/from_bucket")
                                .body(Mono.just(json), JsonTranscodeRequest.class)
                                .retrieve()
                                .onStatus(HttpStatus::isError, clientResponse ->
                                        clientResponse.bodyToMono(String.class) // error body as String or other class
                                                .flatMap(error -> {
                                                    JsonObject response = (JsonObject) JsonParser.parseString(error);
                                                    JsonObject errorElement = (JsonObject) response.get(CommonConstants.ERROR);
                                                    List<String> errors = Collections.singletonList(errorElement.get(CommonConstants.MESSAGE).getAsString());
                                                    String code = errorElement.get(CommonConstants.CODE).getAsString();
                                                    return Mono.error(new BadRequestException("error.transcode.request".concat(code), errors, new Object()));
                                                })
                                )
                                .bodyToMono(String.class)
                                .block();
                    }
                } catch (Exception ex) {
                    LOGGER.error("sendVideoForTranscoding: Couldn't send request to the transcode", ex);
                }
            }
        }
    }

    private void updatePostState(Post post, PostState newState) {
        try {
            PostState oldState = post.getState();
            Long userGroupId = post.getUserGroupId();

            if (!newState.equals(oldState) && !oldState.canBeChangedTo(newState)) {
                throw new ForbiddenException("error.post.state.can.not.be.changed", oldState, newState);
            }

            if (!newState.equals(oldState)) {
                post.setState(newState);
                String messageKey = StringUtils.join("group_", post.getUserGroupId(), "_author_" + post.getAuthorId());
                if (PostState.PUBLISHED.equals(oldState) && Objects.nonNull(userGroupId)) {
                    kafkaProducerService.sendMessage(CommonConstants.POST_EVENT, messageKey, PostEventDTO
                            .builder()
                            .personId(post.getAuthorId())
                            .groupId(post.getUserGroupId())
                            .postId(post.getId())
                            .eventType(PostEventType.POST_UNPUBLISHED)
                            .build());
                }

                if (PostState.PUBLISHED.equals(newState)) {
                    post.setPublishedAt(new Date());
                    kafkaProducerService.sendMessage(CommonConstants.POST_EVENT, messageKey, PostEventDTO
                            .builder()
                            .personId(post.getAuthorId())
                            .groupId(post.getUserGroupId())
                            .postId(post.getId())
                            .postUuid(post.getPostUuid().toString())
                            .publishedAt(post.getPublishedAt())
                            .eventType(PostEventType.POST_PUBLISHED)
                            .build());
                }
            }
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
    }

    private void sendUpdatePostStateNotifications(String authorizationHeader, PersonDTO person, Post post, GroupDTO group, List<Long> mentionedUserIds) {
        PersonData eventAuthor = PersonData
                .builder()
                .id(person.getPersonId())
                .avatar(person.getAvatar())
                .displayName(person.getDisplayName())
                .build();

        PostState newState = post.getState();
        if (PostState.PUBLISHED.equals(newState)) {
            post.setPublishedAt(new Date());
            PersonDTO authorDTO = personManager.getPersonByUuid(post.getAuthorUuid());
            PersonData postAuthor = PersonData
                    .builder()
                    .id(authorDTO.getPersonId())
                    .avatar(authorDTO.getAvatar())
                    .displayName(authorDTO.getDisplayName())
                    .build();

            //Send notification to the post author:
            sendNotification(authorizationHeader, postAuthor, post, group, NotificationType.POST_APPROVED_BY_GROUP_ADMIN, new HashMap<>(), true);

            // New post is published to a group -> send notifications to the following users:
            // - Group members (except post author) who didn't mute the group.
            sendNotification(authorizationHeader, postAuthor, post, group, NotificationType.POST_PUBLISHED_TO_GROUP, new HashMap<>(), false, mentionedUserIds);

            // - Users that follow post author but not members of the group, if the group is public.
            sendNotification(authorizationHeader, postAuthor, post, group, NotificationType.POST_PUBLISHED_TO_GROUP_EXT, new HashMap<>(), false, mentionedUserIds);

            // - Users who are mentioned in the post.
            sendNotification(authorizationHeader, eventAuthor, post, group, NotificationType.PERSON_IS_MENTIONED_IN_POST, new HashMap<>(), false, mentionedUserIds);

        } else if (PostState.REJECTED.equals(newState)) {
            sendNotification(authorizationHeader, eventAuthor, post, group, NotificationType.POST_REJECTED_BY_GROUP_ADMIN, new HashMap<>(), false);
        }
    }

    private Set<Long> getReceiverIds(Post post) {
        // Post state change notification should be sent to group admins, group moderators, and post author (in some cases):
        Set<Long> receiverIds = new HashSet<>();
        if (post.getState().isNotifyAuthor()) {
            receiverIds.add(post.getAuthorId());
        }
        try {
            receiverIds.addAll(groupMemberManager.getGroupMembersByPermissions(post.getUserGroupId(), List.of(Permission.ADMIN, Permission.MODERATOR)).stream().map(GroupMemberDTO::getPersonId).toList());

        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex, new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED), HttpStatus.FORBIDDEN);
        }
        return receiverIds;
    }

    private void validatePostData(PostData data, MultipartFile[] files) {
        if (StringUtils.isBlank(data.getContent()) && (files == null || files.length == 0)) {
            throw new BadRequestException("error.post.content.is.blank");
        }

        if (Objects.nonNull(data.getState()) && data.getState() != PostState.DRAFT) {
            throw new BadRequestException("error.post.initial.state.is.invalid", PostState.DRAFT.name());
        }
    }

    private boolean isAuthor(PersonDTO person, Post post) {
        return Objects.equals(post.getAuthorId(), person.getPersonId());
    }

    /**
     * Returns posts with larger score in the last timeInterval days
     */
    private Page<Post> findTopPostsInTimeInterval(PostSearchCriteria criteria, Pageable page, int timeInterval) {
        return repository.findTopPostsInTimeInterval(criteria, DateUtil.getDateBefore(timeInterval), page);
    }

    @NonNull
    private PostState getPostState(PostingPermission permission, PostState state, boolean isGroupAdmin) {
        if (Objects.isNull(state)) {
            switch (permission) {
                case ADMIN_ONLY, ALL -> state = PostState.PUBLISHED;
                case WITH_APPROVAL -> state = isGroupAdmin ? PostState.PUBLISHED : PostState.WAITING_TO_BE_PUBLISHED;
                default -> state = PostState.WAITING_TO_BE_PUBLISHED;
            }
        }

        return state;
    }

    private void removePostDependencies(PersonDTO person, Post post) {
        // Find post references to remove:
        List<Post> references = repository.findByRepostedFromId(post.getId());
        references.forEach(p -> {
            p.setRepostedFrom(null);
            repository.save(p);
        });
        mediaService.removePostFiles(post.getId());
        postVoteRepository.removePostVotes(Collections.singletonList(post.getId()));
        postStatisticsManager.deletePostStatistics(post.getId());
        postComplaintRepository.deleteByPostId(post.getId());
        removePostFromCollections(person, post);
        removePostFromGroupPostCollections(post);
        commentService.removePostComments(person, post.getId());
    }

    private void removePostFromGroupPostCollections(Post post) {
        List<GroupPostCollection> groupPostCollections = groupPostCollectionRepository.findAllByPostsId(post.getId());
        groupPostCollections.forEach(groupPostCollection -> {
            groupPostCollection.getPosts().remove(getPost(post.getId()));
            groupPostCollectionRepository.save(groupPostCollection);
        });
    }

    private void removePostFromCollections(PersonDTO person, Post post) {
        postCollectionPostsRepository.deleteByPostId(post.getId());
        if (person != null) {
            LOGGER.info("Person: {} removed post: {} from the collections",
                    person.getUuid(), post.getId());
        } else {
            LOGGER.info("Post: {} was removed from the collections due to content moderation",
                    post.getId());
        }
    }

    private Set<Long> getNotificationRecipients(String authorizationHeader, GroupDTO group, Post post, NotificationType type, List<Long> mentionedUserIds) {
        Set<Long> recipients = new HashSet<>();
        switch (type) {
            case POST_CREATED, POST_STATE_CHANGED:
                recipients = getReceiverIds(post);
                break;
            case POST_PUBLISHED_TO_GROUP:
                // - Group members (except post author) who didn't mute the group.
                List<JsonGroupMemberWithNotificationSettings> groupMembers = groupNotificationSettingsManager.getGroupMembersWithSettings(post.getUserGroupId());
                groupMembers = groupMembers.stream().filter(m -> !m.isMuted() && !m.getPersonId().equals(post.getAuthorId())).toList();
                recipients.addAll(groupMembers.stream().map(JsonGroupMemberWithNotificationSettings::getPersonId).toList());
                mentionedUserIds.forEach(recipients::remove);
                break;
            case POST_PUBLISHED_TO_GROUP_EXT:
                // - Users that follow the post author but not members of the group, if the group is public.
                if (AccessType.PUBLIC.equals(group.getAccessType())) {
                    Set<Long> followers = followingRestService.findSubscribers(authorizationHeader, post.getAuthorId(), 0, Integer.MAX_VALUE).stream()
                            .map(f -> f.getSubscriber().getId()).collect(Collectors.toSet());
                    Set<Long> allGroupMembers = groupMemberManager.getAllMemberIdsByGroupId(post.getUserGroupId());
                    followers.removeAll(allGroupMembers);
                    recipients.addAll(followers);
                    mentionedUserIds.forEach(recipients::remove);
                }
                break;
            case POST_DELETED_BY_MODERATOR, POST_APPROVED_BY_GROUP_ADMIN, POST_REJECTED_BY_GROUP_ADMIN, POST_VOTE_ADDED:
                recipients.add(post.getAuthorId());
                break;
            case PERSON_IS_MENTIONED_IN_POST:
                recipients.addAll(filterNotMutedRecipients(mentionedUserIds, post.getId()));
                List<Long> personIdsWhoMutedFollowingId = personRestService.getPersonIdsWhoMutedFollowingId();
                personIdsWhoMutedFollowingId.forEach(recipients::remove);
                break;
            default:
                break;
        }
        return recipients;
    }

    private Set<Long> filterNotMutedRecipients(List<Long> recipients, Long postId) {
        return recipients.stream()
                .filter(r -> !isRecipientMutedPost(r, postId))
                .collect(Collectors.toSet());
    }

    private boolean isRecipientMutedPost(Long personId, Long postId) {
        Optional<PostNotificationSettings> settings = postNotificationSettingsRepository.findByPersonIdAndPostId(personId, postId);
        return settings.isPresent() && settings.get().isMuted();
    }

    private void updateQuery(@Nonnull PostSearchCriteria criteria) {
        String query = criteria.getQuery();
        if (Objects.nonNull(query)) {
            criteria.setQuery("%" + query.toLowerCase().trim().replaceAll("\\s+", "%") + "%");
        } else {
            criteria.setQuery("%");
        }
    }

    @Scheduled(fixedDelayString = "1800000")
    public void jobToProcessAllPublishedPosts() {
        LOGGER.info("Running scheduler to push all published posts to kafka topic");
        Long flag = migrationFlag.opsForValue().get("postsPublishedMigrationFlag");
        if (Objects.isNull(flag) || flag == 0) {
            List<Post> publishedPosts = repository.findByState(PostState.PUBLISHED);
            publishedPosts.forEach(post -> {
                String messageKey = StringUtils.join("group_", post.getUserGroupId(), "_author_" + post.getAuthorId());
                kafkaProducerService.sendMessage(CommonConstants.POST_EVENT, messageKey, PostEventDTO
                            .builder()
                            .personId(post.getAuthorId())
                            .groupId(post.getUserGroupId())
                            .postId(post.getId())
                            .postUuid(post.getPostUuid().toString())
                            .publishedAt(post.getPublishedAt())
                            .eventType(PostEventType.POST_PUBLISHED)
                            .build());
                    });
            migrationFlag.opsForValue().set("postsPublishedMigrationFlag", 1L);
        }
    }
}
