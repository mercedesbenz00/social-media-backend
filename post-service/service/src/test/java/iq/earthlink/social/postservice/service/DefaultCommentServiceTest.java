package iq.earthlink.social.postservice.service;

import iq.earthlink.social.classes.data.dto.ContentModerationDto;
import iq.earthlink.social.classes.enumeration.CommentComplaintState;
import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.file.MediaFileRepository;
import iq.earthlink.social.common.filestorage.CompositeFileStorageProvider;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioFileStorage;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.groupservice.group.rest.UserGroupPermissionRestService;
import iq.earthlink.social.personservice.rest.PersonBanRestService;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.postservice.event.KafkaProducerService;
import iq.earthlink.social.postservice.group.GroupManager;
import iq.earthlink.social.postservice.group.PostingPermission;
import iq.earthlink.social.postservice.group.dto.GroupDTO;
import iq.earthlink.social.postservice.group.member.GroupMemberManager;
import iq.earthlink.social.postservice.group.member.dto.GroupMemberDTO;
import iq.earthlink.social.postservice.person.PersonManager;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.comment.Comment;
import iq.earthlink.social.postservice.post.comment.CommentFileProperties;
import iq.earthlink.social.postservice.post.comment.CommentMediaService;
import iq.earthlink.social.postservice.post.comment.DefaultCommentService;
import iq.earthlink.social.postservice.post.comment.complaint.CommentComplaint;
import iq.earthlink.social.postservice.post.comment.complaint.repository.CommentComplaintRepository;
import iq.earthlink.social.postservice.post.comment.repository.CommentRepository;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettingsRepository;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import iq.earthlink.social.postservice.post.rest.JsonComment;
import iq.earthlink.social.postservice.post.rest.JsonCommentData;
import iq.earthlink.social.postservice.post.vote.repository.CommentVoteRepository;
import iq.earthlink.social.postservice.util.CommentUtil;
import iq.earthlink.social.postservice.util.PermissionUtil;
import iq.earthlink.social.security.config.ServerAuthProperties;
import org.apache.commons.lang3.time.DateUtils;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.dozer.loader.api.FieldsMappingOptions.copyByReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class DefaultCommentServiceTest {

    private static final String COMMENT_CONTENT = "comment content";
    private static final String AUTHORIZATION_HEADER = "authorizationHeader";
    private static final String REASON = "reason";

    @InjectMocks
    private DefaultCommentService commentService;
    @Spy
    private CommentMediaService commentMediaService;
    @Mock
    private MediaFileRepository fileRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentVoteRepository commentVoteRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private FileStorageProvider fileStorageProvider;
    @Mock
    private PersonRestService personRestService;
    @Mock
    private UserGroupPermissionRestService permissionRestService;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private CommentComplaintRepository commentComplaintRepository;
    @Mock
    private ServerAuthProperties authProperties;
    @Mock
    private PostNotificationSettingsRepository postNotificationSettingsRepository;
    @Mock
    private CommentFileProperties properties;
    @Mock
    private PermissionUtil permissionUtil;
    @Mock
    private PersonBanRestService personBanRestService;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Mock
    private GroupMemberManager groupMemberManager;
    @Mock
    private GroupManager groupManager;
    @Mock
    private PersonManager personManager;
    @Mock
    private CommentUtil commentUtil;

    @MockBean
    private MinioFileStorage minioFileStorage;
    @MockBean
    private CachingConnectionFactory connectionFactory;
    @MockBean
    private CompositeFileStorageProvider compositeFileStorageProvider;

    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create()
            .withMappingBuilder(new BeanMappingBuilder() {
                @Override
                protected void configure() {
                    mapping(Comment.class, JsonComment.class)
                            .fields("post.id", "postId")
                            .fields("replyTo.commentUuid", "replyTo", copyByReference())
                            .fields("post.userGroupId", "userGroupId");
                }
            }).build();

    private Long personId;
    private UUID personUuid;
    private UUID postUuid;
    private UUID commentUuid;
    private Long commentId;
    private PersonDTO person;
    private MultipartFile imageFile;
    private Post post;
    private Comment testComment;
    private MediaFile imageFileRecord;
    private GroupDTO groupDTO;
    private JsonCommentData commentData;
    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeEach
    void initTestData() {
        //given
        personId = 1L;
        Long postId = 2L;
        postUuid = UUID.randomUUID();
        commentUuid = UUID.randomUUID();
        personUuid = UUID.randomUUID();
        commentId = 3L;

        person = PersonDTO.builder()
                .personId(personId)
                .uuid(personUuid)
                .build();

        imageFile = new MockMultipartFile("test file", "test file", "image/", new byte[123]);
        post = Post.builder()
                .id(postId)
                .postUuid(postUuid)
                .authorId(personId)
                .authorDisplayName("test author")
                .state(PostState.PUBLISHED)
                .commentsAllowed(true)
                .build();

        testComment = Comment.builder()
                .id(commentId)
                .commentUuid(commentUuid)
                .authorId(personId)
                .authorUuid(personUuid)
                .content(COMMENT_CONTENT)
                .isDeleted(false)
                .createdAt(DateUtils.addDays(new Date(), -1))
                .modifiedAt(new Date())
                .modifiedBy(personId)
                .post(post)
                .build();

        imageFileRecord = MediaFile.builder()
                .ownerId(1L)
                .fileType(MediaFileType.COMMENT_MEDIA)
                .mimeType(imageFile.getContentType())
                .storageType(StorageType.MINIO)
                .size(imageFile.getSize())
                .build();

        groupDTO = new GroupDTO();
        groupDTO.setPostingPermission(PostingPermission.ALL);
        groupDTO.setName("Group name");
        groupDTO.setGroupId(1L);

        commentData = new JsonCommentData();
        commentData.setPostUuid(postUuid);
        commentData.setContent("Post content");
        commentData.setMentionedPersonIds(new ArrayList<>(Collections.singleton(5L)));

        authorizationHeader = AUTHORIZATION_HEADER;

        ReflectionTestUtils.setField(commentService, "NUMBER_OF_REPLIES", 2);
        ReflectionTestUtils.setField(commentService, "TOP_REPLIES_DEPTH", 1);
    }

    @Test
    void createComment_createByValidData_returnComment() {
        //given
        given(fileRepository.save(any(MediaFile.class))).willReturn(imageFileRecord);
        given(commentRepository.saveAndFlush(any(Comment.class))).willReturn(testComment);
        given(commentRepository.findComments(post.getId(), false, Pageable.unpaged())).willReturn(Page.empty());
        given(postRepository.findByPostUuid(commentData.getPostUuid())).willReturn(Optional.ofNullable(post));
        given(permissionUtil.isGroupMember(any(), any())).willReturn(true);
        given(personBanRestService.isPersonBannedFromGroup(any(), any(), any())).willReturn(false);
        given(groupManager.getGroupById(any())).willReturn(groupDTO);
        doNothing().when(rabbitTemplate).convertAndSend(any(), any(), (Object) any());
        given(fileStorageProvider.getStorage(StorageType.MINIO)).willReturn(minioFileStorage);
        given(personManager.getPersonByUuid(any())).willReturn(person);

        //when
        JsonComment commentResult = commentService.createCommentWithFile(null, person, commentData, imageFile);

        //then
        assertNotNull(commentResult);
        assertEquals(commentResult.getId(), testComment.getId());
        assertEquals(commentResult.getAuthorId(), testComment.getAuthorId());
        assertEquals(commentResult.getPostId(), testComment.getPost().getId());
        verify(commentMediaService).uploadCommentFile(commentId, imageFile);
    }

    @Test
    void getCommentByUuid_getByExistingCommentUuid_returnComment() {
        //given
        given(commentRepository.findByCommentUuid(commentUuid)).willReturn(Optional.of(testComment));

        //when
        Comment resultComment = commentService.getCommentByUuidInternal(commentUuid);

        //then
        assertEquals(testComment, resultComment);
    }

    @Test
    void getCommentByUuid_getByNonExistingCommentUuid_throwNotFoundException() {
        //given
        given(commentRepository.findByCommentUuid(commentUuid)).willReturn(Optional.empty());

        //when
        //then
        assertThrows(NotFoundException.class, () -> commentService.getCommentByUuidInternal(commentUuid));
    }

    @Test
    void getCommentWithReplies_getByExistingCommentUuidWithAccess_returnCommentWithReplies() {
        //given
        Comment replyComment1 = Comment.builder()
                .id(2L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .content(COMMENT_CONTENT)
                .isDeleted(false)
                .post(post)
                .replyTo(testComment)
                .build();

        Comment replyComment2 = Comment.builder()
                .id(3L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .content(COMMENT_CONTENT)
                .isDeleted(false)
                .post(post)
                .replyTo(testComment)
                .build();

        List<Comment> replies = Arrays.asList(replyComment1, replyComment2);
        testComment.setReplies(replies);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> replaysPage = new PageImpl<>(replies, pageable, 1);

        given(commentRepository.findReplies(any(), any(), any())).willReturn(replaysPage);
        given(groupMemberManager.getGroupMember(any(), any())).willReturn(GroupMemberDTO.builder().groupId(123L).build());
        given(commentRepository.findByCommentUuid(commentUuid)).willReturn(Optional.of(testComment));
        given(personManager.getPersonByUuid(any())).willReturn(person);

        //when
        JsonComment resultComment = commentService.getCommentWithReplies(personId, commentUuid);

        //then
        assertEquals(2, resultComment.getTopReplies().size());
        assertEquals(testComment.getCommentUuid(), resultComment.getTopReplies().get(0).getReplyTo());
        assertEquals(testComment.getCommentUuid(), resultComment.getTopReplies().get(1).getReplyTo());
    }

    @Test
    void getCommentWithReplies_getByExistingCommentUuidWithoutAccess_throwNotFoundException() {
        //given
        given(commentRepository.findByCommentUuid(commentUuid)).willReturn(Optional.of(testComment));

        //when
        //then
        assertThrows(NotFoundException.class, () -> commentService.getCommentWithReplies(2L, commentUuid));
    }

    @Test
    void findComments_withAccessToGroup_returnPageOfComment() {
        //given
        ReflectionTestUtils.setField(commentService, "TOP_REPLIES_DEPTH", 0);
        Comment replyComment1 = Comment.builder()
                .id(2L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .content(COMMENT_CONTENT)
                .isDeleted(false)
                .post(post)
                .build();

        Comment replyComment2 = Comment.builder()
                .id(3L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .content(COMMENT_CONTENT)
                .isDeleted(false)
                .post(post)
                .build();

        List<Comment> replies = Arrays.asList(replyComment1, replyComment2);
        testComment.setReplies(replies);
        Pageable pageable = PageRequest.of(0, 10);
        List<Comment> commentList = Collections.singletonList(testComment);
        JsonComment jsonComment = mapper.map(testComment, JsonComment.class);

        List<JsonComment> jsonCommentList = Collections.singletonList(jsonComment);
        Page<Comment> commentPage = new PageImpl<>(commentList, pageable, commentList.size());
        Page<JsonComment> jsonCommentPage = new PageImpl<>(jsonCommentList, pageable, jsonCommentList.size());
        Page<Comment> replaysPage = new PageImpl<>(replies, pageable, commentList.size());

        given(groupManager.hasAccessToGroup(anyLong(), anyBoolean(), any())).willReturn(true);
        given(commentRepository.findComments(any(), anyBoolean(), any())).willReturn(commentPage);
        given(commentRepository.findReplies(any(), any(), any())).willReturn(replaysPage);
        given(postRepository.findByPostUuid(commentData.getPostUuid())).willReturn(Optional.ofNullable(post));
        given(personManager.getPersonByUuid(any())).willReturn(person);

        //when
        Page<JsonComment> actualPage = commentService.findComments(person.getPersonId(), person.isAdmin(), postUuid, true, pageable);

        //then
        assertEquals(1, actualPage.getTotalElements());
        assertNotNull(actualPage.getContent().get(0).getAuthor());
        assertNotNull(actualPage.getContent().get(0).getStats());
        assertEquals(personUuid, actualPage.getContent().get(0).getAuthor().getUuid());
        assertEquals(testComment.getId(), Long.valueOf(actualPage.getContent().get(0).getStats().getCommentId()));
        assertEquals(jsonCommentPage.getContent().get(0).getCommentUuid(), actualPage.getContent().get(0).getCommentUuid());
        verify(commentRepository, times(1)).findComments(any(), anyBoolean(), any());
    }

    @Test
    void findComments_withoutAccessToGroup_returnEmptyPage() {
        //given
        Pageable pageable = PageRequest.of(0, 10);given(postRepository.findByPostUuid(commentData.getPostUuid())).willReturn(Optional.ofNullable(post));

        //when
        Page<JsonComment> actualPage = commentService.findComments(person.getPersonId(), person.isAdmin(), postUuid, true, pageable);

        //then
        assertTrue(actualPage.isEmpty());
        verify(commentRepository, times(0)).findComments(any(), anyBoolean(), any());
    }

    @Test
    void findCommentsWithComplaints_findByNullGroupId_returnPageOfComments() {
        //given
        Comment comment1 = Comment.builder()
                .id(2L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .authorUuid(personUuid)
                .content(COMMENT_CONTENT)
                .isDeleted(false)
                .post(post)
                .replyTo(testComment)
                .build();

        Comment comment2 = Comment.builder()
                .id(3L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .authorUuid(personUuid)
                .content(COMMENT_CONTENT)
                .isDeleted(false)
                .post(post)
                .replyTo(testComment)
                .build();

        List<Long> moderatedGroups = Arrays.asList(2L, 3L);
        Pageable page = PageRequest.of(0, 10);
        Page<Comment> comments = new PageImpl<>(Arrays.asList(comment1, comment2), page, 2);

        given(permissionUtil.getModeratedGroups(person.getPersonId())).willReturn(moderatedGroups);
        given(commentComplaintRepository.findCommentsWithComplaints(moderatedGroups, null, false, page)).willReturn(comments);
        given(personManager.getPersonByUuid(any())).willReturn(person);

        //when
        Page<JsonComment> actualComments = commentService.findCommentsWithComplaints(person, null, null, false, page);

        //then
        assertEquals(2, actualComments.getTotalElements());
        verify(commentComplaintRepository).findCommentsWithComplaints(moderatedGroups, null, false, page);
        verify(permissionUtil).getModeratedGroups(person.getPersonId());
    }

    @Test
    void findCommentsWithComplaints_findByNotNullGroupId_returnPageOfComments() {
        //given
        Comment comment1 = Comment.builder()
                .id(2L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .authorUuid(personUuid)
                .content(COMMENT_CONTENT)
                .isDeleted(false)
                .post(post)
                .replyTo(testComment)
                .build();

        Comment comment2 = Comment.builder()
                .id(3L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .authorUuid(personUuid)
                .content(COMMENT_CONTENT)
                .isDeleted(false)
                .post(post)
                .replyTo(testComment)
                .build();

        Long groupId = 123L;
        List<Long> groupIds = List.of(groupId);
        Pageable page = PageRequest.of(0, 10);
        Page<Comment> comments = new PageImpl<>(Arrays.asList(comment1, comment2), page, 2);

        doNothing().when(permissionUtil).checkGroupPermissions(any(PersonDTO.class), any());
        given(commentComplaintRepository.findCommentsWithComplaints(groupIds, null, false, page)).willReturn(comments);
        given(personManager.getPersonByUuid(any())).willReturn(person);

        //when
        Page<JsonComment> actualComments = commentService.findCommentsWithComplaints(person, groupId, null, false, page);

        //then
        assertEquals(2, actualComments.getTotalElements());
        verify(commentComplaintRepository).findCommentsWithComplaints(groupIds, null, false, page);
        verify(permissionUtil, never()).getModeratedGroups(any());
        verify(permissionUtil).checkGroupPermissions(any(PersonDTO.class), any());
    }

    @Test
    void replyWithFile_replyWithValidData_returnReplyComment() {
        //given
        String replyContent = "reply content";
        Comment replyComment = Comment.builder()
                .id(234L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .content(replyContent)
                .isDeleted(false)
                .post(post)
                .replyTo(testComment)
                .build();

        Page<Comment> replaysPage = new PageImpl<>(Collections.singletonList(replyComment), Pageable.unpaged(), 2);

        given(commentRepository.findByCommentUuid(commentUuid)).willReturn(Optional.of(testComment));
        given(commentRepository.save(any(Comment.class))).willReturn(replyComment);
        given(commentRepository.findReplies(any(), any(), any())).willReturn(replaysPage);
        given(groupManager.getGroupById(any())).willReturn(groupDTO);
        given(personManager.getPersonByUuid(any())).willReturn(person);

        //when
        JsonComment reply = commentService.reply(authorizationHeader, person, commentUuid, commentData, imageFile);

        //then
        assertNotNull(reply);
        assertEquals(person.getPersonId(), reply.getAuthorId());
        assertEquals(replyContent, reply.getContent());
        assertEquals(testComment.getCommentUuid(), reply.getReplyTo());
        assertEquals(testComment.getPost().getId(), reply.getPostId());
    }

    @Test
    void replyWithFile_replyToNonExistentComment_throwNotFoundException() {
        //given
        given(commentRepository.findByCommentUuid(any())).willReturn(Optional.empty());

        //when
        //then
        assertThrows(NotFoundException.class, () -> commentService.reply(authorizationHeader, person, commentUuid, commentData, imageFile));
    }

    @Test
    void editWithFile_editWithValidData_returnEditedComment() {
        //given
        JsonCommentData newData = new JsonCommentData();
        newData.setContent("New comment content");
        testComment.setContent(newData.getContent());

        given(commentRepository.findByCommentUuid(commentUuid)).willReturn(Optional.of(testComment));
        given(commentRepository.save(any())).willReturn(testComment);
        given(personManager.getPersonByUuid(any())).willReturn(person);

        //when
        JsonComment editedComment = commentService.editWithFile(authorizationHeader, person, testComment.getCommentUuid(), newData, imageFile);

        //then
        assertEquals(newData.getContent(), editedComment.getContent());
        then(commentMediaService).should().uploadCommentFile(any(Long.class), any(MultipartFile.class));
    }

    @Test
    void rejectCommentByComplaint_rejectWithValidData_commentRejected() {
        //given
        PersonDTO requester = new PersonDTO();
        CommentComplaint complaint = new CommentComplaint();
        complaint.setState(CommentComplaintState.PENDING);
        complaint.setComment(testComment);
        UUID complaintUuid = UUID.randomUUID();

        given(commentComplaintRepository.findByComplaintUuid(complaintUuid)).willReturn(Optional.of(complaint));
        given(commentRepository.findById(any())).willReturn(Optional.ofNullable(testComment));
        given(postRepository.findById(any())).willReturn(Optional.ofNullable(post));
        given(groupManager.getGroupById(any())).willReturn(groupDTO);
        doNothing().when(permissionUtil).checkGroupPermissions(requester, testComment.getPost().getUserGroupId());

        //when
        commentService.rejectCommentByComplaint(requester, REASON, complaintUuid);

        //then
        assertTrue(testComment.isDeleted());
        verify(commentRepository).save(testComment);
    }

    @Test
    void rejectCommentByComplaint_complaintAlreadyResolved_throwBadRequestException() {
        //given
        PersonDTO requester = new PersonDTO();
        CommentComplaint complaint = new CommentComplaint();
        complaint.setState(CommentComplaintState.COMMENT_REJECTED);
        complaint.setComment(testComment);
        UUID complaintUuid = UUID.randomUUID();

        //when
        given(commentComplaintRepository.findByComplaintUuid(complaintUuid)).willReturn(Optional.of(complaint));

        //then
        assertThrows(BadRequestException.class, () -> commentService.rejectCommentByComplaint(requester, REASON, complaintUuid));
        verify(commentComplaintRepository, Mockito.never()).save(Mockito.any(CommentComplaint.class));
    }

    @Test
    void rejectCommentByComplaint_rejectByNonExistingComplaintUuid_throwNotFoundException() {
        //given
        PersonDTO requester = new PersonDTO();
        UUID complaintUuid = UUID.randomUUID();

        //when
        given(commentComplaintRepository.findByComplaintUuid(complaintUuid)).willReturn(Optional.empty());

        //then
        assertThrows(NotFoundException.class, () -> commentService.rejectCommentByComplaint(requester, REASON, complaintUuid));
        Mockito.verify(commentComplaintRepository, Mockito.never()).save(Mockito.any(CommentComplaint.class));
    }

    @Test
    void removeComment_removeCommentAsAuthor_commentRemoved() {
        //given
        given(commentRepository.findByCommentUuid(testComment.getCommentUuid())).willReturn(Optional.of(testComment));
        given(commentRepository.findReplies(any(), any(), any())).willReturn(Page.empty());

        //when
        commentService.removeComment(person, testComment.getCommentUuid());

        //then
        Mockito.verify(commentRepository, Mockito.times(1)).delete(testComment);
    }

    @Test
    void removeComment_removeCommentAsNonAuthor_throwForbiddenException() {
        //given
        testComment.setAuthorId(345L);
        given(commentRepository.findByCommentUuid(testComment.getCommentUuid())).willReturn(Optional.of(testComment));
        UUID commentUUID = testComment.getCommentUuid();
        //when
        //then
        assertThrows(ForbiddenException.class, () -> commentService.removeComment(person, commentUUID));
    }

    @Test
    void getReplies_getByNullCommentUuid_returnEmptyPage() {
        //when

        Page<JsonComment> page = commentService.getReplies(person.getPersonId(), person.isAdmin(), null, true, Pageable.unpaged());

        //then
        assertTrue(page.isEmpty());
    }

    @Test
    void getReplies_getByPersonWithoutAccess_throwNotFoundException() {
        //given
        given(commentRepository.findReplies(anyLong(), anyBoolean(), any(Pageable.class))).willReturn(Page.empty());

        //when
        //then
        Pageable page = Pageable.unpaged();
        boolean isAdmin = person.isAdmin();
        assertThrows(NotFoundException.class, () -> commentService.getReplies(personId, isAdmin, commentUuid, true, page));
    }

    @Test
    void getReplies_getByValidData_returnPageOfReplies() {
        //given
        Comment replyComment1 = Comment.builder()
                .id(2L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .content(COMMENT_CONTENT)
                .isDeleted(false)
                .replyTo(testComment)
                .post(post)
                .build();

        Comment replyComment2 = Comment.builder()
                .id(3L)
                .commentUuid(UUID.randomUUID())
                .authorId(personId)
                .content("comment content 2")
                .isDeleted(false)
                .replyTo(testComment)
                .post(post)
                .build();

        List<Comment> replies = Arrays.asList(replyComment1, replyComment2);
        testComment.setReplies(replies);
        Pageable pageable = PageRequest.of(0, 10);
        List<Comment> commentList = Collections.singletonList(testComment);
        Page<Comment> replaysPage = new PageImpl<>(replies, pageable, commentList.size());

        given(commentRepository.findByCommentUuid(commentUuid)).willReturn(Optional.of(testComment));
        given(commentRepository.findReplies(any(), any(), any())).willReturn(replaysPage);
        given(groupManager.hasAccessToGroup(anyLong(), anyBoolean(), any())).willReturn(true);
        given(personManager.getPersonByUuid(any())).willReturn(person);

        //when
        Page<JsonComment> page = commentService.getReplies(person.getPersonId(), person.isAdmin(), commentUuid, true, Pageable.unpaged());

        //then
        assertFalse(page.isEmpty());
        assertEquals(2, page.getTotalElements());
        assertEquals(replyComment1.getId(), page.getContent().get(0).getId());
        assertEquals(replyComment2.getContent(), page.getContent().get(1).getContent());
    }

    @Test
    void removeCommentByModerator_removeByValidData_commentRemoved() {
        //given
        ContentModerationDto dto = new ContentModerationDto("type", testComment.getId());

        given(commentRepository.findById(testComment.getId())).willReturn(Optional.of(testComment));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(commentRepository.findReplies(any(), any(), any())).willReturn(Page.empty());
        given(groupManager.getGroupById(any())).willReturn(groupDTO);

        //when
        commentService.removeCommentByModerator(dto);

        //then
        verify(commentRepository).delete(testComment);
    }

    @Test
    void removePostComments_removeByNullPerson_commentsDeletedByModerator() {
        //given
        Pageable pageable = PageRequest.of(0, 10);
        List<Comment> commentList = Collections.singletonList(testComment);
        Page<Comment> expectedPage = new PageImpl<>(commentList, pageable, commentList.size());
        given(commentRepository.findById(testComment.getId())).willReturn(Optional.of(testComment));
        given(commentRepository.findComments(any(), anyBoolean(), any())).willReturn(expectedPage);
        given(commentRepository.findByCommentUuid(commentUuid)).willReturn(Optional.of(testComment));
        given(commentRepository.findReplies(any(), any(), any())).willReturn(Page.empty());

        //when
        commentService.removePostComments(null, post.getId());

        //then
        verify(commentRepository).delete(testComment);

    }

    @Test
    void removePostComments_removeByNotNullPerson_commentsDeleted() {
        //given
        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        List<Comment> commentList = Collections.singletonList(testComment);
        Page<Comment> expectedPage = new PageImpl<>(commentList, pageable, commentList.size());
        given(commentRepository.findById(testComment.getId())).willReturn(Optional.of(testComment));
        given(commentRepository.findComments(any(), anyBoolean(), any())).willReturn(expectedPage);
        given(commentRepository.findByCommentUuid(commentUuid)).willReturn(Optional.of(testComment));
        given(commentRepository.findReplies(any(), any(), any())).willReturn(Page.empty());

        //when
        commentService.removePostComments(person, post.getId());

        //then
        verify(commentRepository).delete(testComment);
    }

    @Test
    void deleteMediaFile_deleteCommentMediaFile_mediaFileDeleted() {
        //given
        given(commentRepository.findByCommentUuid(testComment.getCommentUuid())).willReturn(Optional.of(testComment));

        //when
        commentService.deleteMediaFile(authorizationHeader, person, testComment.getCommentUuid());

        //then
        verify(commentMediaService, times(1)).removeCommentFile(testComment.getId());
    }
}
