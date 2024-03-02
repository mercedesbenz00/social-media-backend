package iq.earthlink.social.postservice.post;

import iq.earthlink.social.classes.data.dto.ContentModerationDto;
import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.postservice.group.AccessType;
import iq.earthlink.social.postservice.group.dto.GroupDTO;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.JsonPost;
import iq.earthlink.social.postservice.post.rest.PostStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PostManager {

    @Nonnull
    JsonPost getPost(Long personId, boolean isAdmin, @Nonnull Long postId);

    @Nonnull
    Post getPost(@Nonnull Long postId);

    @Nonnull
    Post getPostByUuid(@Nonnull UUID postUuid);

    @Nonnull
    JsonPost createPostDeprecated(String authorizationHeader, @Nonnull PersonDTO personDTO, @Nonnull PostData data, @Nullable MultipartFile[] files);

    Post createPostInternal(String authorizationHeader,
                                   @Nonnull PersonDTO personDTO,
                                   @Nonnull PostData postData,
                                   @Nullable MultipartFile[] files);

    @Nonnull
    Page<JsonPost> findPostsDeprecated(Long personId, @Nonnull PostSearchCriteria criteria, @Nonnull Pageable page);

    @Nonnull
    Page<Post> findPostsInternal(@Nonnull PostSearchCriteria criteria, @Nonnull Pageable page);

    @Nonnull
    JsonPost updatePostDeprecated(String authorizationHeader, @Nonnull PersonDTO personDTO, @Nonnull Long postId, UpdatePostData data, MultipartFile[] files);

    Post update(String authorizationHeader,
                       @Nonnull PersonDTO person,
                       @Nonnull Long postId,
                       UpdatePostData data,
                       MultipartFile[] files);

    void removePost(@Nonnull PersonDTO person, @Nonnull Long postId);

    void removePostFile(@Nonnull PersonDTO person, @Nonnull Long postId, @Nonnull Long fileId);

    void removeLinkMeta(@Nonnull PersonDTO person, @Nonnull Long postId);

    Page<JsonPost> findPostsWithComplaintsDeprecated(String authorizationHeader, PersonDTO person, Long groupId, PostComplaintState complainState, PostState postState, Pageable page);

    Page<Post> findPostsWithComplaintsInternal(String authorizationHeader, PersonDTO person, Long groupId, PostComplaintState complaintState, PostState postState, Pageable page);

    @Nonnull
    Post rejectPostByComplaintDeprecated(String authorizationHeader, PersonDTO person, String reason, Long complaintId);

    void updatePostsAuthorDisplayName(Long authorId, String displayName);

    void updatePostsUserGroupType(Long userGroupId, AccessType accessType);

    void updatePostGroupType(Long userGroupId, AccessType userGroupType);

    /**
     * Returns post statistics.
     *
     * @param fromDate,     String
     * @param timeInterval, enum - Time interval for which statistical results are calculated: DAY, MONTH, or YEAR. Default value - MONTH.
     */
    PostStats getPostStats(String fromDate, TimeInterval timeInterval);

    void removePostByModerator(@Nonnull ContentModerationDto dto);

    void sendNotification(String authorizationHeader, PersonData person, @Nonnull Post post, @Nonnull GroupDTO group,
                          @Nonnull NotificationType type, @Nonnull Map<String, String> additionalMetadata,
                          boolean sendToEventAuthor);

    void sendNotification(String authorizationHeader, PersonData eventAuthor, @Nonnull Post post, @Nonnull GroupDTO group,
                          @Nonnull NotificationType type, @Nonnull Map<String, String> additionalMetadata,
                          boolean sendToEventAuthor, List<Long> mentionedUserIds);

    List<Long> getFrequentlyPostsGroups(Long personId);

}
