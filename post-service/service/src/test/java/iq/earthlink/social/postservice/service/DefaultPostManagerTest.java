package iq.earthlink.social.postservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.classes.enumeration.SortType;
import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.common.filestorage.minio.MinioProperties;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.groupservice.group.rest.JsonMemberPermission;
import iq.earthlink.social.groupservice.group.rest.MembersRestService;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.personservice.rest.PersonBanRestService;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.postservice.event.KafkaProducerService;
import iq.earthlink.social.postservice.group.GroupManager;
import iq.earthlink.social.postservice.group.PostingPermission;
import iq.earthlink.social.postservice.group.dto.GroupDTO;
import iq.earthlink.social.postservice.group.member.GroupMemberManager;
import iq.earthlink.social.postservice.group.member.dto.GroupMemberDTO;
import iq.earthlink.social.postservice.group.notificationsettings.GroupNotificationSettingsManager;
import iq.earthlink.social.postservice.person.PersonManager;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.*;
import iq.earthlink.social.postservice.post.collection.repository.GroupPostCollectionRepository;
import iq.earthlink.social.postservice.post.collection.repository.PostCollectionPostsRepository;
import iq.earthlink.social.postservice.post.collection.repository.PostCollectionRepository;
import iq.earthlink.social.postservice.post.comment.Comment;
import iq.earthlink.social.postservice.post.comment.CommentService;
import iq.earthlink.social.postservice.post.comment.complaint.repository.CommentComplaintRepository;
import iq.earthlink.social.postservice.post.comment.repository.CommentRepository;
import iq.earthlink.social.postservice.post.complaint.model.PostComplaint;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import iq.earthlink.social.postservice.post.complaint.repository.PostComplaintRepository;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettings;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettingsRepository;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import iq.earthlink.social.postservice.post.rest.*;
import iq.earthlink.social.postservice.post.statistics.PostStatisticsManager;
import iq.earthlink.social.postservice.post.vote.PostVoteManager;
import iq.earthlink.social.postservice.post.vote.repository.PostVoteRepository;
import iq.earthlink.social.postservice.util.PermissionUtil;
import iq.earthlink.social.postservice.util.PostStatisticsAndMediaUtil;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsLastArg;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class DefaultPostManagerTest {

    private static final String CONTENT = "some content";
    private static final String AUTHOR_DISPLAY_NAME = "author 1";
    private static final String PUSH_NOTIFICATION = "PUSH_NOTIFICATION";
    private static final String USER_NAME = "abc1234";
    private static final String UPDATED_CONTENT = "updated content";
    private static final String ORIGINAL_CONTENT = "original content";

    @InjectMocks
    private DefaultPostManager postManager;

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private MembersRestService membersRestService;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CompositeMeterRegistry meterRegistry;
    @Mock
    private Counter counter;
    @Mock
    private PostComplaintRepository postComplaintRepository;
    @Mock
    private PostMediaService mediaService;
    @Mock
    private PostStatisticsManager postStatisticsManager;
    @Mock
    private PostVoteManager postVoteManager;
    @Mock
    private PersonBanRestService personBanRestService;
    @Mock
    private PersonRestService personRestService;
    @Mock
    private PermissionUtil permissionUtil;
    @Mock
    private MinioProperties minioProperties;
    @Mock
    private PostVoteRepository postVoteRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentService commentService;
    @Mock
    private PostCollectionRepository postCollectionRepository;
    @Mock
    private CommentComplaintRepository commentComplaintRepository;
    @Mock
    private GroupPostCollectionRepository groupPostCollectionRepository;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Mock
    private PostNotificationSettingsRepository postNotificationSettingsRepository;
    @Mock
    private PostCollectionPostsRepository postCollectionPostsRepository;
    @Mock
    private PostStatisticsAndMediaUtil postStatisticsAndMediaUtil;
    @Mock
    private PersonManager personManager;
    @Mock
    private GroupManager groupManager;
    @Mock
    private GroupMemberManager groupMemberManager;
    @Mock
    private GroupNotificationSettingsManager groupNotificationSettingsManager;

    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @Captor
    private ArgumentCaptor<NotificationEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void whenFindPostByInvalidId_throwError() {
        // 1. Find post by invalid ID:
        Long postId = Long.MAX_VALUE;
        assertThatThrownBy(() -> postManager.getPost(postId))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.not.found.post");
    }

    @Test
    void whenFindPostByValidId_returnPostInfo() {
        Post post = Post.builder()
                .id(1L)
                .authorId(1L)
                .authorDisplayName("test author")
                .build();

        // 1. Find post by valid ID:
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        Post foundPostById = postManager.getPost(post.getId());

        assertEquals(foundPostById.getId(), post.getId());
        assertEquals(foundPostById.getAuthorId(), post.getAuthorId());
        assertEquals(foundPostById.getAuthorDisplayName(), post.getAuthorDisplayName());
    }

    @Test
    void whenFindPostByInvalidUuid_throwError() {
        // 1. Find post by invalid UUID:
        UUID postUuid = new UUID(0, 0);
        assertThatThrownBy(() -> postManager.getPostByUuid(postUuid))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.not.found.post");
    }

    @Test
    void whenFindPostByValidUuid_returnPostInfo() {
        Post post = Post.builder()
                .id(1L)
                .postUuid(new UUID(0, 0))
                .authorId(1L)
                .authorDisplayName("test author")
                .build();

        // 1. Find post by valid UUID:
        given(postRepository.findByPostUuid(any())).willReturn(Optional.of(post));

        Post foundPostById = postManager.getPostByUuid(post.getPostUuid());

        assertEquals(foundPostById.getPostUuid(), post.getPostUuid());
        assertEquals(foundPostById.getAuthorId(), post.getAuthorId());
        assertEquals(foundPostById.getAuthorDisplayName(), post.getAuthorDisplayName());
    }

    @Test
    void whenCreatePostByUserWhoBannedAtThisGroup_throwError() {
        PersonDTO personDTO = PersonDTO.builder().personId(1L).build();
        PostData postData = JsonPostData.builder().userGroupId(1L).build();

        // 1. Trying to create a post in a group where person was banned:
        given(personBanRestService.isPersonBannedFromGroup("", postData.getUserGroupId(), personDTO.getPersonId())).willReturn(true);

        assertThatThrownBy(() -> postManager.createPostDeprecated("", personDTO, postData, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.add.post.user.banned");
    }

    @Test
    void whenCreatePostByUserWhoNotAGroupMember_throwError() {
        PersonDTO personDTO = PersonDTO.builder().personId(1L).build();
        PostData postData = JsonPostData.builder().userGroupId(1L).build();

        // 1. Trying to create a post in a group in which person is not a member:
        given(personBanRestService.isPersonBannedFromGroup("", postData.getUserGroupId(), personDTO.getPersonId())).willReturn(false);

        assertThatThrownBy(() -> postManager.createPostDeprecated("", personDTO, postData, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.person.can.not.create.post");
    }

    @Test
    void whenCreatePostByUserInGroupWithPostingPermissionAdminOnly_throwError() {
        PersonDTO personDTO = PersonDTO.builder()
                .personId(1L)
                .build();

        PostData postData = JsonPostData.builder()
                .userGroupId(1L)
                .content(CONTENT)
                .state(PostState.DRAFT)
                .build();

        GroupMemberDTO memberInfo = GroupMemberDTO.builder()
                .build();

        var groupDTO = GroupDTO
                .builder()
                .groupId(1L)
                .postingPermission(PostingPermission.ALL)
                .build();

        // 1. Trying to create a post in a group with the PostingPermission - ADMIN_ONLY without administrator rights:
        given(personBanRestService.isPersonBannedFromGroup("", postData.getUserGroupId(), personDTO.getPersonId())).willReturn(false);
        given(permissionUtil.isGroupAdminOrModerator(memberInfo)).willReturn(false);
        given(groupManager.getGroupById(any())).willReturn(groupDTO);


        assertThatThrownBy(() -> postManager.createPostDeprecated("", personDTO, postData, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("error.person.can.not.create.post");
    }

    @Test
    void whenCreatePostWithValidData_returnPost() {
        PersonDTO personDTO = PersonDTO.builder()
                .personId(1L)
                .build();

        PostData postData = JsonPostData.builder()
                .userGroupId(1L)
                .content(CONTENT)
                .state(PostState.DRAFT)
                .build();

        GroupMemberDTO memberInfo = GroupMemberDTO.builder()
                .build();

        Post post = Post.builder()
                .id(1L)
                .authorId(1L)
                .authorDisplayName(AUTHOR_DISPLAY_NAME)
                .state(PostState.PUBLISHED)
                .userGroupId(1L)
                .publishedAt(DateUtil.getDateBefore(3))
                .build();

        var groupDTO = GroupDTO
                .builder()
                .groupId(1L)
                .postingPermission(PostingPermission.ALL)
                .build();

        // 1. Trying to create a post with valid data:
        given(personBanRestService.isPersonBannedFromGroup("", postData.getUserGroupId(), personDTO.getPersonId())).willReturn(false);
        given(groupMemberManager.getGroupMember(any(), any())).willReturn(memberInfo);
        given(permissionUtil.isGroupAdminOrModerator(memberInfo)).willReturn(false);
        given(groupManager.getGroupById(any())).willReturn(groupDTO);
        given(postRepository.save(any())).willReturn(post);
        doNothing().when(rabbitTemplate).convertAndSend(any(), any(), (Object) any());
        given(meterRegistry.counter(any())).willReturn(counter);
        JsonPost createdPost = postManager.createPostDeprecated("", personDTO, postData, null);

        assertNotNull(createdPost);
        assertEquals(createdPost.getPostUuid(), post.getPostUuid());
        assertEquals(createdPost.getAuthorId(), post.getAuthorId());
        assertEquals(createdPost.getAuthorDisplayName(), post.getAuthorDisplayName());
    }

    @Test
    void createPost_createWithMentionedIds_notificationSentToMentionedUsers() {
        //given
        PersonDTO personDTO = PersonDTO.builder()
                .personId(1L)
                .build();

        PostData postData = JsonPostData.builder()
                .userGroupId(1L)
                .content(CONTENT)
                .mentionedPersonIds(List.of(2L, 3L))
                .build();

        GroupMemberDTO memberInfo = GroupMemberDTO.builder()
                .build();

        Post post = Post.builder()
                .id(1L)
                .postUuid(UUID.randomUUID())
                .authorId(1L)
                .authorDisplayName(AUTHOR_DISPLAY_NAME)
                .state(PostState.PUBLISHED)
                .userGroupId(1L)
                .publishedAt(DateUtil.getDateBefore(3))
                .build();

        var groupDTO = GroupDTO
                .builder()
                .groupId(1L)
                .postingPermission(iq.earthlink.social.postservice.group.PostingPermission.ALL)
                .build();

        given(personBanRestService.isPersonBannedFromGroup("", postData.getUserGroupId(), personDTO.getPersonId())).willReturn(false);
        given(groupMemberManager.getGroupMember(groupDTO.getGroupId(), personDTO.getPersonId())).willReturn(memberInfo);
        given(groupManager.getGroupById(any())).willReturn(groupDTO);
        given(postRepository.save(any())).willReturn(post);
        given(meterRegistry.counter(any())).willReturn(counter);
        doNothing().when(rabbitTemplate).convertAndSend(any(), any(), (Object) any());

        //when
        JsonPost createdPost = postManager.createPostDeprecated("", personDTO, postData, null);

        //then
        verify(kafkaProducerService, times(1)).sendMessage(eq(PUSH_NOTIFICATION), eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();
        assertEquals(List.of(2L, 3L), capturedEvent.getReceiverIds());
        assertEquals(NotificationType.PERSON_IS_MENTIONED_IN_POST, capturedEvent.getType());
        assertNotNull(createdPost);
    }

   /* @Test
    void createPost_createWithMentionedIdsButOneOfThemMuted_notificationSentToMentionedUsersExceptMuted() {
        //given
        PersonDTO personDTO = PersonDTO.builder()
                .personId(1L)
                .build();

        PostData postData = JsonPostData.builder()
                .userGroupId(1L)
                .content(CONTENT)
                .mentionedPersonIds(List.of(2L, 3L, 4L))
                .build();

        GroupMemberDTO memberInfo = GroupMemberDTO.builder()
                .build();

        Post post = Post.builder()
                .id(1L)
                .postUuid(UUID.randomUUID())
                .authorId(1L)
                .authorDisplayName(AUTHOR_DISPLAY_NAME)
                .state(PostState.PUBLISHED)
                .userGroupId(1L)
                .publishedAt(DateUtil.getDateBefore(3))
                .build();

        JsonPost jsonPost = JsonPost.builder()
                .id(1L)
                .postUuid(post.getPostUuid())
                .authorId(1L)
                .authorDisplayName(AUTHOR_DISPLAY_NAME)
                .state(PostState.PUBLISHED)
                .userGroupId(1L)
                .publishedAt(post.getPublishedAt())
                .build();

        var groupDTO = GroupDTO
                .builder()
                .groupId(1L)
                .postingPermission(PostingPermission.ALL)
                .build();

        given(personBanRestService.isPersonBannedFromGroup("", postData.getUserGroupId(), personDTO.getPersonId())).willReturn(false);
        given(groupMemberManager.getGroupMember(any(), any())).willReturn(memberInfo);
        given(permissionUtil.isGroupAdminOrModerator(memberInfo)).willReturn(false);
        given(groupManager.getGroupById(any())).willReturn(groupDTO);
        given(postRepository.save(any())).willReturn(post);
        given(personRestService.getPersonIdsWhoMutedFollowingId()).willReturn(List.of(2L));
        given(meterRegistry.counter(any())).willReturn(counter);
        doNothing().when(rabbitTemplate).convertAndSend(any(), any(), (Object) any());

        //when
        JsonPost createdPost = postManager.createPostDeprecated("", personDTO, postData, null);

        //then
        verify(kafkaProducerService, times(1)).sendMessage(any(), eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();
        assertEquals(List.of(3L, 4L), capturedEvent.getReceiverIds());
        assertEquals(NotificationType.PERSON_IS_MENTIONED_IN_POST, capturedEvent.getType());
        assertNotNull(createdPost);
    }*/

    @Test
    void whenFindPostsByCriteria_returnPaginatedResult() {

        // 1. Find posts based on query, groupId, state and authorId criteria:
        PostSearchCriteria criteria1 = PostSearchCriteria.builder()
                .query(AUTHOR_DISPLAY_NAME)
                .groupIds(List.of(1L, 2L))
                .states(List.of(PostState.PUBLISHED))
                .authorIds(List.of(1L, 2L))
                .build();

        PersonInfo person = JsonPersonProfile.builder()
                .id(1L)
                .email("test@abc.com")
                .username(USER_NAME)
                .displayName(USER_NAME)
                .isVerifiedAccount(true)
                .roles(Set.of("ADMIN"))
                .build();

        List<Post> posts = getPosts().stream().filter(p ->
                p.getAuthorDisplayName().contains(criteria1.getQuery())
                        && criteria1.getGroupIds().contains(p.getUserGroupId()) && criteria1.getStates().contains(p.getState())
                        && criteria1.getAuthorIds().contains(p.getAuthorId())).collect(Collectors.toList());
        Page<Post> page1 = new PageImpl<>(posts, PageRequest.of(0, 3), posts.size());

        // given
        given(postRepository.findPosts(criteria1, page1.getPageable())).willReturn(page1);

        Page<JsonPost> foundPosts1 = postManager.findPostsDeprecated(person.getId(), criteria1, page1.getPageable());

        assertTrue(foundPosts1.isFirst());
        assertEquals(1, foundPosts1.getTotalPages());
        assertEquals(posts.size(), foundPosts1.getContent().size());
        assertEquals(posts.size(), foundPosts1.getTotalElements());

        // 2. Find the newest posts:
        List<Post> allPosts = new ArrayList<>(getPosts());

        PostSearchCriteria criteria3 = PostSearchCriteria.builder()
                .sortType(SortType.NEWEST)
                .build();

        List<Post> newestPosts = allPosts.stream().sorted(Comparator.comparing(Post::getPublishedAt).reversed())
                .collect(Collectors.toList());
        Page<Post> page3 = new PageImpl<>(newestPosts, PageRequest.of(0, 3, Sort.Direction.DESC,
                Post.PUBLISHED_AT, Post.CREATED_AT), posts.size());

        // given
        given(postRepository.findPosts(criteria3, page3.getPageable())).willReturn(page3);
        given(postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(any(Long.class), any())).will(returnsLastArg());

        Page<JsonPost> foundPosts3 = postManager.findPostsDeprecated(person.getId(), criteria3, page3.getPageable());

        assertTrue(foundPosts3.isFirst());
        assertEquals(5L, (long) foundPosts3.getContent().get(0).getId());
        assertEquals(2, foundPosts3.getTotalPages());
        assertEquals(allPosts.size(), foundPosts3.getContent().size());
        assertEquals(allPosts.size(), foundPosts3.getTotalElements());
    }

    @Test
    void whenFindPostsWithComplaints_returnPaginatedResult() {

        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .displayName(USER_NAME)
                .isVerifiedAccount(true)
                .roles(Set.of("ADMIN"))
                .build();

        // 1. Find users with pending complaints:
        List<PostComplaint> pendingComplaints = getPostPendingComplaints();
        Page<Post> page1 = new PageImpl<>(pendingComplaints.stream().map(PostComplaint::getPost).collect(Collectors.toList()),
                PageRequest.of(0, 3), pendingComplaints.size());

        Pageable pageable = page1.getPageable();
        given(postComplaintRepository.findPostsWithComplaints(List.of(1L), PostComplaintState.PENDING,
                PostState.PUBLISHED, pageable))
                .willReturn(page1);
        Page<JsonPost> foundPosts1 = postManager.findPostsWithComplaintsDeprecated("", person, 1L,
                PostComplaintState.PENDING, PostState.PUBLISHED, pageable);

        assertTrue(foundPosts1.isFirst());
        assertEquals(2, foundPosts1.getTotalPages());
        assertEquals(pendingComplaints.size(), foundPosts1.getContent().size());
        assertEquals(pendingComplaints.size(), foundPosts1.getTotalElements());

        // 2. Find users with complaint state not provided:
        assertThatThrownBy(() -> postManager.findPostsWithComplaintsDeprecated("", person, 1L,
                null, null, pageable))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.check.not.null");
    }

    @Test
    void updatePost_ByAuthor_WithValidData_returnPost() {
        PersonDTO person = PersonDTO.builder()
                .uuid(UUID.randomUUID())
                .personId(1L)
                .build();

        UpdatePostData postData = JsonUpdatePostData.builder()
                .content(UPDATED_CONTENT)
                .commentsAllowed(false)
                .state(PostState.PUBLISHED)
                .shouldPin(true)
                .build();

        MultipartFile[] files = new MultipartFile[]{new MockMultipartFile("file", new byte[]{})};
        UUID uuid = UUID.randomUUID();
        Post post = Post.builder()
                .id(1L)
                .authorUuid(person.getUuid())
                .authorId(person.getPersonId())
                .postUuid(uuid)
                .content(ORIGINAL_CONTENT)
                .commentsAllowed(true)
                .state(PostState.DRAFT)
                .userGroupId(1L)
                .build();

        var groupDTO = GroupDTO
                .builder()
                .groupId(1L)
                .postingPermission(PostingPermission.ALL)
                .build();
        JsonMemberPermission memberInfo = JsonMemberPermission.builder()
                .state(ApprovalState.APPROVED)
                .build();

        JsonPost expectedJsonPost = new JsonPost();
        expectedJsonPost.setId(post.getId());
        expectedJsonPost.setAuthorId(person.getPersonId());
        expectedJsonPost.setPostUuid(uuid);
        expectedJsonPost.setContent(UPDATED_CONTENT);
        expectedJsonPost.setCommentsAllowed(false);
        expectedJsonPost.setState(PostState.PUBLISHED);

        // 1. Update post with valid data:
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(personBanRestService.isPersonBannedFromGroup(anyString(), anyLong(), anyLong())).willReturn(false);
        given(permissionUtil.hasGroupPermissions(any(PersonDTO.class), anyLong())).willReturn(true);
        given(permissionUtil.isGroupAdminOrModerator(any(GroupMemberDTO.class))).willReturn(true);
        given(groupManager.getGroupById(anyLong())).willReturn(groupDTO);
        given(mediaService.uploadPostFiles(any(), any())).willReturn(Collections.emptyList());
        given(postRepository.save(any())).willReturn(post);
        given(personManager.getPersonByUuid(any(UUID.class))).willReturn(person);
        given(postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(any(Long.class), any())).will(returnsLastArg());
        // Call the method under test
        JsonPost actualJsonPost = postManager.updatePostDeprecated("", person, post.getId(), postData, files);

        // Verify the result
        assertNotNull(actualJsonPost);
        assertEquals(expectedJsonPost.getPostUuid(), actualJsonPost.getPostUuid());
        assertEquals(expectedJsonPost.getAuthorId(), actualJsonPost.getAuthorId());
        assertEquals(expectedJsonPost.getAuthorDisplayName(), actualJsonPost.getAuthorDisplayName());
        assertEquals(expectedJsonPost.getContent(), actualJsonPost.getContent());
        assertEquals(expectedJsonPost.getState(), actualJsonPost.getState());

        // Verify the behavior of the dependencies
        verify(postRepository).findById(post.getId());
        verify(personBanRestService).isPersonBannedFromGroup("", post.getUserGroupId(), person.getPersonId());
        verify(permissionUtil).hasGroupPermissions(person, post.getUserGroupId());
        verify(mediaService).uploadPostFiles(post, files);
        verify(postRepository).save(post);
    }

    @Test
    void updatePost_withValidDataAndNewMentionedIds_notificationSentOnlyForNewMentionedIds() {
        //given
        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .build();

        UpdatePostData postData = JsonUpdatePostData.builder()
                .content(UPDATED_CONTENT)
                .commentsAllowed(false)
                .state(PostState.PUBLISHED)
                .mentionedPersonIds(List.of(2L, 3L, 4L, 5L))
                .shouldPin(true)
                .build();

        UUID uuid = UUID.randomUUID();
        Post post = Post.builder()
                .id(1L)
                .authorId(person.getPersonId())
                .postUuid(uuid)
                .content(ORIGINAL_CONTENT)
                .commentsAllowed(true)
                .state(PostState.PUBLISHED)
                .userGroupId(1L)
                .mentionedPersonIds(List.of(1L, 2L, 4L))
                .build();

        var groupDTO = GroupDTO
                .builder()
                .groupId(1L)
                .postingPermission(PostingPermission.ALL)
                .build();

        JsonMemberPermission memberInfo = JsonMemberPermission.builder()
                .state(ApprovalState.APPROVED)
                .build();

        JsonPost expectedJsonPost = new JsonPost();
        expectedJsonPost.setId(post.getId());
        expectedJsonPost.setAuthorId(person.getPersonId());
        expectedJsonPost.setPostUuid(uuid);
        expectedJsonPost.setContent(UPDATED_CONTENT);
        expectedJsonPost.setCommentsAllowed(false);
        expectedJsonPost.setState(PostState.PUBLISHED);

        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(personBanRestService.isPersonBannedFromGroup(anyString(), anyLong(), anyLong())).willReturn(false);
        given(permissionUtil.hasGroupPermissions(any(PersonDTO.class), anyLong())).willReturn(true);
        given(permissionUtil.isGroupAdminOrModerator(any(GroupMemberDTO.class))).willReturn(true);
        given(groupManager.getGroupById(anyLong())).willReturn(groupDTO);
        given(mediaService.uploadPostFiles(any(), any())).willReturn(Collections.emptyList());
        given(postRepository.save(any())).willReturn(post);
        given(postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(any(Long.class), any())).will(returnsLastArg());

        //when
        JsonPost actualJsonPost = postManager.updatePostDeprecated("", person, post.getId(), postData, null);

        //then
        assertNotNull(actualJsonPost);
        verify(kafkaProducerService, times(1)).sendMessage(eq(PUSH_NOTIFICATION), eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();
        assertEquals(List.of(3L, 5L), capturedEvent.getReceiverIds());
        assertEquals(NotificationType.PERSON_IS_MENTIONED_IN_POST, capturedEvent.getType());
    }

    @Test
    void updatePost_WithMentionedIdsButOneOfThemMuted_notificationSentToMentionedUsersExceptMuted() {
        //given
        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .build();

        UpdatePostData postData = JsonUpdatePostData.builder()
                .content(UPDATED_CONTENT)
                .commentsAllowed(false)
                .state(PostState.PUBLISHED)
                .mentionedPersonIds(List.of(2L, 3L, 4L, 5L))
                .shouldPin(true)
                .build();

        UUID uuid = UUID.randomUUID();
        Post post = Post.builder()
                .id(1L)
                .postUuid(uuid)
                .authorId(person.getPersonId())
                .content(ORIGINAL_CONTENT)
                .commentsAllowed(true)
                .state(PostState.PUBLISHED)
                .userGroupId(1L)
                .mentionedPersonIds(List.of(1L, 4L))
                .build();

        var groupDTO = GroupDTO
                .builder()
                .groupId(1L)
                .postingPermission(PostingPermission.ALL)
                .build();
        JsonMemberPermission memberInfo = JsonMemberPermission.builder()
                .state(ApprovalState.APPROVED)
                .build();

        JsonPost expectedJsonPost = new JsonPost();
        expectedJsonPost.setId(post.getId());
        expectedJsonPost.setAuthorId(person.getPersonId());
        expectedJsonPost.setPostUuid(uuid);
        expectedJsonPost.setContent(UPDATED_CONTENT);
        expectedJsonPost.setCommentsAllowed(false);
        expectedJsonPost.setState(PostState.PUBLISHED);

        PostNotificationSettings postNotificationSettings = PostNotificationSettings.builder()
                .postId(post.getId())
                .personId(2L)
                .isMuted(true)
                .build();

        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(personBanRestService.isPersonBannedFromGroup(anyString(), anyLong(), anyLong())).willReturn(false);
        given(permissionUtil.hasGroupPermissions(any(PersonDTO.class), anyLong())).willReturn(true);
        given(permissionUtil.isGroupAdminOrModerator(any(GroupMemberDTO.class))).willReturn(true);
        given(groupManager.getGroupById(anyLong())).willReturn(groupDTO);
        given(mediaService.uploadPostFiles(any(), any())).willReturn(Collections.emptyList());
        given(postRepository.save(any())).willReturn(post);
        given(postNotificationSettingsRepository.findByPersonIdAndPostId(2L, post.getId())).willReturn(Optional.of(postNotificationSettings));
        given(postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(any(Long.class), any())).will(returnsLastArg());

        //when
        JsonPost actualJsonPost = postManager.updatePostDeprecated("", person, post.getId(), postData, null);

        //then
        assertNotNull(actualJsonPost);
        verify(kafkaProducerService, times(1)).sendMessage(eq(PUSH_NOTIFICATION), eventCaptor.capture());
        NotificationEvent capturedEvent = eventCaptor.getValue();
        assertEquals(List.of(3L, 5L), capturedEvent.getReceiverIds());
        assertEquals(NotificationType.PERSON_IS_MENTIONED_IN_POST, capturedEvent.getType());
    }

    @Test
    void updatePost_ByAdminUser_changeContent_throwForbidden() {
        //given
        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .roles(Set.of("ADMIN"))
                .build();

        UpdatePostData postData = JsonUpdatePostData.builder()
                .content(UPDATED_CONTENT)
                .build();

        UUID uuid = UUID.randomUUID();
        Post post = Post.builder()
                .id(1L)
                .postUuid(uuid)
                .authorId(2L)
                .content(ORIGINAL_CONTENT)
                .commentsAllowed(true)
                .state(PostState.PUBLISHED)
                .userGroupId(1L)
                .mentionedPersonIds(List.of(1L, 4L))
                .build();
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));

        //when then
        assertThrows(ForbiddenException.class, () -> postManager.updatePostDeprecated("", person, 1L, postData, null));
    }

    @Test
    void whenRejectPostByComplaint_thenPostRejectedAndComplaintUpdated() {
        Long complaintId = 1L;
        Long personId = 2L;
        Long groupId = 1L;
        String reason = "Inappropriate content";

        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();

        Post post = Post.builder().id(1L).userGroupId(groupId).postUuid(UUID.randomUUID()).build();
        PostComplaint complaint = PostComplaint.builder()
                .id(complaintId)
                .state(PostComplaintState.PENDING)
                .post(post)
                .build();
        var groupDto = GroupDTO
                .builder()
                .groupId(groupId)
                .postingPermission(iq.earthlink.social.postservice.group.PostingPermission.ALL)
                .build();

        // 1. Reject post by complaint:
        given(postComplaintRepository.findById(complaintId)).willReturn(Optional.of(complaint));
        doNothing().when(permissionUtil).checkGroupPermissions(person, complaint.getPost().getUserGroupId());
        given(postRepository.save(complaint.getPost())).willReturn(complaint.getPost());
        given(groupManager.getGroupById(groupId)).willReturn(groupDto);

        Post rejectedPost = postManager.rejectPostByComplaintDeprecated("authorizationHeader", person, reason, complaintId);

        assertThat(rejectedPost.getState()).isEqualTo(PostState.REJECTED);
        assertThat(rejectedPost.getStateChangedDate()).isNotNull();
        verify(postComplaintRepository).resolvePendingPostComplaints(post.getId(), person.getPersonId(), reason);
    }

    @Test
    void removePost_shouldDeletePostIfPersonHasPermissions() {
        Long postId = 1L;

        Long personId = 2L;

        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();

        Post post = Post.builder()
                .id(postId)
                .authorId(33L)
                .userGroupId(1L)
                .build();

        // Remove post with permission:
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(permissionUtil.hasGroupPermissions(person, post.getUserGroupId())).willReturn(true);
        given(commentRepository.findByPostId(postId, Pageable.unpaged())).willReturn(Page.empty());
        given(commentRepository.findComments(postId, true, Pageable.unpaged())).willReturn(Page.empty());

        postManager.removePost(person, postId);

        verify(postRepository).delete(post);
    }

    @Test
    void removePostWithCommentFromOtherPerson_shouldDeletePostAndComment() {
        Long postId = 1L;
        Long postAuthorId = 2L;
        Long commentAuthorId = 3L;
        Long commentId = 4L;

        PersonDTO postAuthor = PersonDTO.builder()
                .personId(postAuthorId)
                .build();

        Post post = Post.builder()
                .id(postId)
                .authorId(postAuthorId)
                .userGroupId(1L)
                .build();

        Comment comment = Comment.builder()
                .id(commentId)
                .commentUuid(UUID.randomUUID())
                .authorId(commentAuthorId)
                .content("comment content")
                .isDeleted(false)
                .post(post)
                .build();

        PageImpl<Comment> commentPage = new PageImpl<>(Collections.singletonList(comment));

        // Remove post with comment:
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(permissionUtil.hasGroupPermissions(postAuthor, post.getUserGroupId())).willReturn(true);
        given(commentRepository.findByPostId(postId, Pageable.unpaged())).willReturn(Page.empty());
        given(commentRepository.findComments(postId, true, Pageable.unpaged())).willReturn(commentPage);

        postManager.removePost(postAuthor, postId);

        verify(postRepository).delete(post);
        verify(commentService).removePostComments(any(), any());
    }

    @Test
    void removePost_shouldDeletePostIfPersonIsAuthor() {
        Long postId = 1L;

        Long personId = 2L;

        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();

        Post post = Post.builder()
                .id(postId)
                .authorId(personId)
                .userGroupId(1L)
                .build();

        // Remove post when person is author:
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(permissionUtil.hasGroupPermissions(person, post.getUserGroupId())).willReturn(false);
        given(commentRepository.findByPostId(postId, Pageable.unpaged())).willReturn(Page.empty());
        given(commentRepository.findComments(postId, true, Pageable.unpaged())).willReturn(Page.empty());

        postManager.removePost(person, postId);

        verify(postRepository).delete(post);
    }

    @Test
    void removePost_shouldThrowForbiddenExceptionIfPersonDoesNotHavePermissions() {
        Long postId = 1L;

        Long personId = 2L;

        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();

        Post post = Post.builder()
                .id(postId)
                .authorId(33L)
                .userGroupId(1L)
                .build();

        // Remove post without permission:
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(permissionUtil.hasGroupPermissions(person, post.getUserGroupId())).willReturn(false);

        assertThrows(ForbiddenException.class, () -> postManager.removePost(person, postId));
        verify(postRepository, never()).delete(post);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void whenGetPostStats_returnPostStats() {
        LocalDateTime localDateTimeNow = LocalDateTime.now();
        List<CreatedPosts> createdPostsPerDay = new ArrayList<>();
        createdPostsPerDay.add(new CreatedPosts(localDateTimeNow.toString(), 2));
        createdPostsPerDay.add(new CreatedPosts(localDateTimeNow.minusDays(1).toString(), 1));

        // Get post stats for day TimeInterval:
        when(postRepository.getAllPostsCount()).thenReturn(10L);
        when(postRepository.getNewPostsCount(any(Timestamp.class))).thenReturn(5L);
        when(postRepository.getCreatedPostsPerDay(any(Timestamp.class))).thenReturn(createdPostsPerDay);

        PostStats result = postManager.getPostStats("2022-03-01", TimeInterval.DAY);

        assertEquals(10L, result.getAllPostsCount());
        assertEquals(5L, result.getNewPostsCount());
        assertEquals(Timestamp.valueOf(LocalDateTime.of(2022, 3, 1, 0, 0)), result.getFromDate());
        assertEquals(TimeInterval.DAY, result.getTimeInterval());

        List<CreatedPosts> createdPosts = result.getCreatedPosts();
        assertEquals(2, createdPosts.size());
        assertEquals(2, createdPosts.get(0).getCreatedPostsCount());
        assertEquals(localDateTimeNow.toString(), createdPosts.get(0).getDate());
        assertEquals(1, createdPosts.get(1).getCreatedPostsCount());
        assertEquals(localDateTimeNow.minusDays(1).toString(), createdPosts.get(1).getDate());
    }

    private List<Post> getPosts() {
        Post post1 = Post.builder()
                .id(1L)
                .authorId(1L)
                .authorDisplayName(AUTHOR_DISPLAY_NAME)
                .state(PostState.PUBLISHED)
                .userGroupId(1L)
                .publishedAt(DateUtil.getDateBefore(3))
                .build();

        Post post2 = Post.builder()
                .id(2L)
                .authorId(2L)
                .authorDisplayName("author 2")
                .state(PostState.PUBLISHED)
                .userGroupId(2L)
                .publishedAt(DateUtil.getDateBefore(2))
                .build();

        Post post3 = Post.builder()
                .id(3L)
                .authorId(1L)
                .authorDisplayName(AUTHOR_DISPLAY_NAME)
                .state(PostState.WAITING_TO_BE_PUBLISHED)
                .userGroupId(3L)
                .publishedAt(DateUtil.getDateBefore(5))
                .build();

        Post post4 = Post.builder()
                .id(4L)
                .authorId(1L)
                .authorDisplayName(AUTHOR_DISPLAY_NAME)
                .state(PostState.PUBLISHED)
                .userGroupId(4L)
                .publishedAt(DateUtil.getDateBefore(30))
                .build();

        Post post5 = Post.builder()
                .id(5L)
                .authorId(1L)
                .authorDisplayName(AUTHOR_DISPLAY_NAME)
                .state(PostState.PUBLISHED)
                .userGroupId(2L)
                .publishedAt(DateUtil.getDateBefore(1))
                .build();

        Post post6 = Post.builder()
                .id(6L)
                .authorId(2L)
                .authorDisplayName("author 2")
                .state(PostState.WAITING_TO_BE_PUBLISHED)
                .userGroupId(2L)
                .publishedAt(DateUtil.getDateBefore(2))
                .build();


        List<Post> posts = new ArrayList<>();
        posts.add(post1);
        posts.add(post2);
        posts.add(post3);
        posts.add(post4);
        posts.add(post5);
        posts.add(post6);
        return posts;
    }

    private List<PostComplaint> getPostPendingComplaints() {
        List<PostComplaint> postComplaints = new ArrayList<>();
        Reason reason = Reason.builder()
                .id(1L)
                .name("test reason")
                .build();

        getPosts().forEach(p -> {
            p.setState(PostState.PUBLISHED);
            p.setUserGroupId(1L);
            PostComplaint c = PostComplaint.builder()
                    // .id(1L)
                    .post(p)
                    .reason(reason)
                    .state(PostComplaintState.PENDING)
                    .build();
            postComplaints.add(c);
        });

        return postComplaints;
    }
}
