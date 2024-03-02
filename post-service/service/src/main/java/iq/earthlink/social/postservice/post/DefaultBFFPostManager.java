package iq.earthlink.social.postservice.post;

import io.micrometer.core.annotation.Timed;
import iq.earthlink.social.classes.data.dto.*;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.classes.enumeration.SortType;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.postservice.group.AccessType;
import iq.earthlink.social.postservice.group.GroupManager;
import iq.earthlink.social.postservice.group.dto.GroupDTO;
import iq.earthlink.social.postservice.group.member.GroupMemberManager;
import iq.earthlink.social.postservice.person.PersonManager;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.comment.CommentService;
import iq.earthlink.social.postservice.post.dto.PostPublicDTO;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import iq.earthlink.social.postservice.post.statistics.PostStatisticsManager;
import iq.earthlink.social.postservice.post.vote.PostVote;
import iq.earthlink.social.postservice.post.vote.repository.PostVoteRepository;
import iq.earthlink.social.postservice.util.PermissionUtil;
import iq.earthlink.social.security.SecurityProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * This is the backend for frontend manager and all methods should be used only by rest API controller
 **/
@Service
@RequiredArgsConstructor
public class DefaultBFFPostManager implements BFFPostManager {
    private final PostRepository repository;
    private final PersonManager personManager;
    private final GroupManager groupManager;
    private final GroupMemberManager groupMemberManager;
    private final PostMediaService mediaService;
    private final PostManager postManager;
    private final PostStatisticsManager postStatisticsManager;
    private final PostVoteRepository postVoteRepository;
    private final CommentService commentService;
    private final SecurityProvider securityProvider;
    private final PermissionUtil permissionUtil;
    private static final String ERROR_NOT_FOUND_POST = "error.not.found.post";

    @Transactional
    @Nonnull
    @Override
    public PostResponse createPost(String authorizationHeader, @NotNull PersonDTO personDTO, @NotNull PostData postData, @Nullable MultipartFile[] files) {
        var post = postManager.createPostInternal(authorizationHeader, personDTO, postData, files);
        return getPostResponse(personDTO.getPersonId(), post);
    }

    @NotNull
    @Override
    public PostResponse updatePost(String authorizationHeader, @NotNull PersonDTO person, @NotNull Long postId, UpdatePostData data, MultipartFile[] files) {
        var updatedPost = postManager.update(authorizationHeader, person, postId, data, files);
        return getPostResponse(person.getPersonId(), updatedPost);
    }

    @Override
    @Nonnull
    public PostResponse getPostById(PersonDTO currentUser, @Nonnull Long postId) {
        Post post = repository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_POST, postId));

        if (!PostState.PUBLISHED.equals(post.getState()) && !Objects.equals(currentUser.getPersonId(), post.getAuthorId()) && !permissionUtil.hasGroupPermissions(currentUser, post.getUserGroupId())) {
            throw new NotFoundException(ERROR_NOT_FOUND_POST, postId);
        }
        var userGroup = groupManager.getGroupById(post.getUserGroupId());
        if (userGroup.getAccessType().equals(AccessType.PRIVATE) && Objects.isNull(groupMemberManager.getGroupMember(post.getUserGroupId(), currentUser.getPersonId()))) {
            throw new NotFoundException(ERROR_NOT_FOUND_POST, postId);
        }
        return getPostResponse(currentUser.getPersonId(), post);
    }

    @Override
    public Page<PostResponse> findPosts(PersonDTO currentUser, @NotNull PostSearchCriteria criteria, @NotNull Pageable page) {
        updateQuery(criteria);
        criteria.setUserId(currentUser.getPersonId());
        var postStates = criteria.getStates();
        if ((criteria.getSortType() == null || List.of(SortType.ALL, SortType.NEWEST).contains(criteria.getSortType())) &&
                CollectionUtils.isNotEmpty(postStates) && !Collections.disjoint(postStates, List.of(PostState.WAITING_TO_BE_PUBLISHED, PostState.DRAFT, PostState.REJECTED)) && !currentUser.isAdmin()) {
            criteria.setAuthorIds(List.of(currentUser.getPersonId()));
        }
        var posts = postManager.findPostsInternal(criteria, page);
        var postResponses = getPostsResponses(currentUser.getPersonId(), posts.getContent());
        return new PageImpl<>(postResponses, page, posts.getTotalElements());
    }

    @Override
    @Timed(value = "postService.get.posts.with.details", description = "Time taken to fetch posts with details by post uuids")
    public List<PostResponse> getPostsWithDetailsInternal(String authorizationHeader, List<UUID> postUuids) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        List<Post> posts = repository.findByPostUuidIn(postUuids);
        return getPostsResponses(currentUserId, posts);
    }

    @Override
    public Page<PostResponse> findPostsWithComplaints(String authorizationHeader, PersonDTO person, Long groupId, PostComplaintState complaintState, PostState postState, Pageable page) {
        var posts = postManager.findPostsWithComplaintsInternal(authorizationHeader, person, groupId, complaintState, postState, page);
        var postResponseList = getPostsResponses(person.getPersonId(), posts.getContent());
        return new PageImpl<>(postResponseList, page, posts.getTotalElements());
    }

    @NotNull
    @Override
    public PostResponse rejectPostByComplaint(String authorizationHeader, PersonDTO person, String reason, Long complaintId) {
        var post = postManager.rejectPostByComplaintDeprecated(authorizationHeader, person, reason, complaintId);
        return getPostResponse(person.getPersonId(), post);
    }

    @Override
    public PostPublicDTO getPostByUuid(UUID postUuid) {

        Post post = repository.findByPostUuid(postUuid)
                .orElseThrow(() -> new NotFoundException(ERROR_NOT_FOUND_POST, postUuid));
        if (AccessType.PRIVATE.equals(post.getUserGroupType()) || !PostState.PUBLISHED.equals(post.getState())) {
            throw new NotFoundException(ERROR_NOT_FOUND_POST, postUuid);
        }
        var enrichedPost = getPostResponse(null, post);
        var comments = commentService.getCommentsInternal(post.getId());

        return PostPublicDTO
                .builder()
                .id(enrichedPost.getId())
                .postUuid(enrichedPost.getPostUuid())
                .files(enrichedPost.getFiles())
                .author(enrichedPost.getAuthor())
                .postType(enrichedPost.getPostType())
                .linkMeta(enrichedPost.getLinkMeta())
                .repostedFrom(enrichedPost.getRepostedFrom())
                .stats(enrichedPost.getStats())
                .content(enrichedPost.getContent())
                .group(enrichedPost.getGroup())
                .publishedAt(enrichedPost.getPublishedAt())
                .createdAt(enrichedPost.getCreatedAt())
                .comments(comments)
                .build();

    }

    @NotNull
    private PostResponse getPostResponse(Long currentUserId, Post post) {
        var postResponse = PostResponse.builder()
                .id(post.getId())
                .postUuid(post.getPostUuid().toString())
                .content(post.getContent())
                .group(GroupDetails.builder().id(post.getUserGroupId()).build())
                .author(AuthorDetails.builder().id(post.getAuthorId()).uuid(post.getAuthorUuid()).build())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .postType(post.getPostType().name())
                .state(post.getState())
                .commentsAllowed(post.isCommentsAllowed())
                .linkMeta(post.getLinkMeta())
                .build();
        enrichPostsWithDetails(currentUserId, List.of(postResponse));
        return postResponse;
    }

    @NotNull
    private List<PostResponse> getPostsResponses(Long currentUserId, List<Post> posts) {
        var postResponseList = posts.stream()
                .map(post -> PostResponse.builder()
                        .id(post.getId())
                        .postUuid(post.getPostUuid().toString())
                        .content(post.getContent())
                        .group(GroupDetails.builder().id(post.getUserGroupId()).build())
                        .author(AuthorDetails.builder().id(post.getAuthorId()).uuid(post.getAuthorUuid()).build())
                        .publishedAt(post.getPublishedAt())
                        .createdAt(post.getCreatedAt())
                        .postType(post.getPostType().name())
                        .state(post.getState())
                        .commentsAllowed(post.isCommentsAllowed())
                        .linkMeta(post.getLinkMeta())
                        .build()).toList();
        enrichPostsWithDetails(currentUserId, postResponseList);
        return postResponseList;
    }

    public void enrichPostsWithDetails(Long currentUserId, List<PostResponse> postResponseList) {
        Set<UUID> authorUuids = new HashSet<>();
        Set<Long> groupIds = new HashSet<>();
        List<Long> postIds = new ArrayList<>();
        postResponseList.forEach(postDetails -> {
            authorUuids.add(postDetails.getAuthor().getUuid());
            groupIds.add(postDetails.getGroup().getId());
            postIds.add(postDetails.getId());
        });
        var groupDetailsFuture = getGroupsDetailsByGroupIds(groupIds);
        var authorsDetails = getAuthorsDetailsByAuthorIds(authorUuids);
        var mediaFiles = getMediaFilesByPostIds(postIds);
        var postStats = getPostStatisticsByPostIds(currentUserId, postIds);
        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(groupDetailsFuture, authorsDetails, mediaFiles, postStats);
        combinedFuture.thenAccept(unused -> {

            // Get the results of the completed operations
            List<GroupDetails> groupDetailsList = new ArrayList<>();
            List<AuthorDetails> authorDetailsList = new ArrayList<>();
            List<JsonMediaFile> mediaFilesList = new ArrayList<>();
            List<PostDetailStatistics> postStatsList = new ArrayList<>();
            try {
                groupDetailsList = groupDetailsFuture.get();
                authorDetailsList = authorsDetails.get();
                mediaFilesList = mediaFiles.get();
                postStatsList = postStats.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            aggregatePostWithDetails(postResponseList, groupDetailsList, authorDetailsList, mediaFilesList, postStatsList);
        });
        try {
            combinedFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Async
    @Timed(value = "postService.get.group.details", description = "Time taken to fetch groups from the group service by groupIds")
    public CompletableFuture<List<GroupDetails>> getGroupsDetailsByGroupIds(Set<Long> groupIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<GroupDetails> groupDetails = new ArrayList<>();
            try {
                List<GroupDTO> groupDTOS = groupManager.getGroupsByIds(List.copyOf(groupIds));
                groupDetails = groupDTOS.stream().map(group -> GroupDetails
                        .builder()
                        .id(group.getGroupId())
                        .name(group.getName())
                        .avatar(getAvatarDetails(group.getAvatar()))
                        .build()).toList();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return groupDetails;
        });
    }

    @Async
    @Timed(value = "postService.get.authors.details", description = "Time taken to fetch users from the person service by user ids")
    public CompletableFuture<List<AuthorDetails>> getAuthorsDetailsByAuthorIds(Set<UUID> authorUuids) {
        return CompletableFuture.supplyAsync(() -> {
            List<AuthorDetails> authorDetailsList = new ArrayList<>();
            try {
                var personList = personManager.getPersonsByUuids(List.copyOf(authorUuids));
                authorDetailsList = personList.stream().map(person -> AuthorDetails
                        .builder()
                        .id(person.getPersonId())
                        .uuid(person.getUuid())
                        .displayName(person.getDisplayName())
                        .avatar(getAvatarDetails(person.getAvatar()))
                        .isVerified(person.isVerifiedAccount())
                        .build()).toList();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return authorDetailsList;
        });
    }

    @Async
    @Timed(value = "postService.get.media.files", description = "Time taken to fetch media files by post ids")
    public CompletableFuture<List<JsonMediaFile>> getMediaFilesByPostIds(List<Long> postIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<JsonMediaFile> mediaFiles = new ArrayList<>();
            try {
                mediaFiles = mediaService.findFilesByPostIds(postIds);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return mediaFiles;
        });
    }

    @Async
    @Timed(value = "postService.get.posts.statistics", description = "Time taken to fetch post statistics by post ids")
    public CompletableFuture<List<PostDetailStatistics>> getPostStatisticsByPostIds(Long currentUserId, List<Long> postIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<PostDetailStatistics> postDetailStatistics = new ArrayList<>();
            try {
                postDetailStatistics = postStatisticsManager.getPostStatisticsByPostIds(postIds);
                if (currentUserId != null) {
                    List<PostVote> votes = postVoteRepository.getPersonPostVotesForPosts(currentUserId, postIds);
                    List<PostDetailStatistics> finalPostDetailStatistics = postDetailStatistics;
                    votes.forEach(vote -> finalPostDetailStatistics.stream()
                            .filter(stat -> stat.getPostId() == vote.getId().getPost().getId())
                            .forEach(stat -> stat.setVoteValue(vote.getVoteType())));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return postDetailStatistics;
        });
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

    public void aggregatePostWithDetails(List<PostResponse> postResponseList,
                                         List<GroupDetails> groupDetailsList,
                                         List<AuthorDetails> authorDetailsList,
                                         List<JsonMediaFile> mediaFilesList,
                                         List<PostDetailStatistics> postStatsList) {
        postResponseList.forEach(postDetails -> {
            authorDetailsList.forEach(authorDetails -> {
                if (authorDetails.getUuid().equals(postDetails.getAuthor().getUuid())) {
                    var author = postDetails.getAuthor();
                    author.setAvatar(authorDetails.getAvatar());
                    author.setDisplayName(authorDetails.getDisplayName());
                    author.setIsVerified(authorDetails.getIsVerified());
                }
            });
            var postMediaFiles = mediaFilesList.stream().filter(file -> Objects.equals(file.getOwnerId(), postDetails.getId())).toList();
            if (!CollectionUtils.isEmpty(postMediaFiles)) {
                postDetails.setFiles(postMediaFiles);
            }
            var postDetailStats = postStatsList.stream().filter(stat -> Objects.equals(stat.getPostId(), postDetails.getId())).toList();
            if (!CollectionUtils.isEmpty(postDetailStats)) {
                postDetails.setStats(postDetailStats.get(0));
            } else {
                postDetails.setStats(new PostDetailStatistics());
            }
            groupDetailsList.forEach(groupDetails -> {
                if (groupDetails.getId().equals(postDetails.getGroup().getId())) {
                    var group = postDetails.getGroup();
                    group.setAvatar(groupDetails.getAvatar());
                    group.setName(groupDetails.getName());
                }
            });
        });
    }

    private void updateQuery(@Nonnull PostSearchCriteria criteria) {
        String query = criteria.getQuery();
        if (Objects.nonNull(query)) {
            criteria.setQuery("%" + query.toLowerCase().trim().replaceAll("\\s+", "%") + "%");
        } else {
            criteria.setQuery("%");
        }
    }
}
