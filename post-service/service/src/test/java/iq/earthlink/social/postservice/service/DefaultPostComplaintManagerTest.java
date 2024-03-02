package iq.earthlink.social.postservice.service;

import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.postservice.person.PersonManager;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.PostComplaintData;
import iq.earthlink.social.postservice.post.PostComplaintState;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.complaint.DefaultPostComplaintManager;
import iq.earthlink.social.postservice.post.complaint.ReasonManager;
import iq.earthlink.social.postservice.post.complaint.model.PostComplaint;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import iq.earthlink.social.postservice.post.complaint.repository.PostComplaintRepository;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.JsonPostComplaint;
import iq.earthlink.social.postservice.post.rest.JsonPostComplaintData;
import iq.earthlink.social.postservice.post.rest.JsonReason;
import iq.earthlink.social.postservice.util.PermissionUtil;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class DefaultPostComplaintManagerTest {

    private static final String ERROR_NOT_FOUND_COMPLAINT = "error.not.found.complaint";
    private static final String REJECT_REASON = "reject reason";


    @InjectMocks
    private DefaultPostComplaintManager complaintManager;

    @Mock
    private PostComplaintRepository complaintRepository;
    @Mock
    private PermissionUtil permissionUtil;
    @Mock
    private PostManager postManager;
    @Mock
    private ReasonManager reasonManager;
    @Mock
    private PersonManager personManager;

    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create()
            .withMappingBuilder(new BeanMappingBuilder() {
                @Override
                protected void configure() {
                    mapping(PostComplaint.class, JsonPostComplaint.class)
                            .fields("post.id", "postId");
                }
            }).build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void createComplaint_emptyReason_throwException() {
        Post post = getPost(1L, 2L, PostState.PUBLISHED);
        PostComplaintData data = JsonPostComplaintData.builder().build();

        assertThatThrownBy(() -> complaintManager.createComplaint(2L, post, data))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.post.complaint.reason.not.provided");
    }

    @Test
    void createComplaint_invalidPost_throwException() {
        Post post = getPost(1L, 2L, PostState.WAITING_TO_BE_PUBLISHED);

        PostComplaintData data = JsonPostComplaintData.builder()
                .reason(getReason())
                .build();

        assertThatThrownBy(() -> complaintManager.createComplaint(2L, post, data))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.post.complaint.wrong.post.state");
    }

    @Test
    void createComplaint_duplicate_throwException() {
        PersonDTO person = getPerson(2L);
        Post post = getPost(1L, 2L, PostState.PUBLISHED);

        PostComplaintData data = JsonPostComplaintData.builder()
                .reason(getReason())
                .build();

        PostComplaint pendingComplaint = PostComplaint.builder().post(post).authorId(person.getPersonId()).state(PostComplaintState.PENDING).build();
        given(complaintRepository.findByAuthorIdAndPostId(person.getPersonId(), post.getId())).willReturn(Optional.of(pendingComplaint));

        assertThatThrownBy(() -> complaintManager.createComplaint(2L, post, data))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.post.complaint.already.created");

        PostComplaint resolvedComplaint = PostComplaint.builder().post(post).authorId(person.getPersonId()).state(PostComplaintState.POST_REJECTED).build();
        given(complaintRepository.findByAuthorIdAndPostId(person.getPersonId(), post.getId())).willReturn(Optional.of(resolvedComplaint));

        assertThatThrownBy(() -> complaintManager.createComplaint(2L, post, data))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.post.complaint.already.created");
    }

    @Test
    void createComplaint_withReason_returnPostComplaint() {
        PersonDTO person = getPerson(2L);
        Post post = getPost(1L, 2L, PostState.PUBLISHED);
        Reason reason = mapper.map(getReason(), Reason.class);
        PostComplaintData data = JsonPostComplaintData.builder()
                .reason(getReason())
                .build();

        given(complaintRepository.findByAuthorIdAndPostId(person.getPersonId(), post.getId())).willReturn(Optional.empty());
        given(reasonManager.getComplaintReason(data.getReason().getId())).willReturn(reason);

        JsonPostComplaint actualComplaint = complaintManager.createComplaint(2L, post, data);

        assertEquals(getReason().getId(), actualComplaint.getReason().getId());
        assertEquals(PostComplaintState.PENDING, actualComplaint.getState());

    }

    @Test
    void getComplaint_notFound_throwException() {
        PersonDTO person = getPerson(2L);
        Post post = getPost(1L, 2L, PostState.PUBLISHED);

        given(complaintRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> complaintManager.getJsonPostComplaint(person.getPersonId(), post, 1L))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining(ERROR_NOT_FOUND_COMPLAINT);
    }

    @Test
    void getComplaint_found_returnPostComplaint() {
        PersonDTO person = getPerson(2L);
        Post post = getPost(1L, 2L, PostState.PUBLISHED);
        Reason reason = mapper.map(getReason(), Reason.class);
        PostComplaint expectedComplaint = PostComplaint.builder()
                .id(1L)
                .post(post)
                .authorId(person.getPersonId())
                .reason(reason)
                .state(PostComplaintState.PENDING)
                .build();

        given(complaintRepository.findById(1L)).willReturn(Optional.of(expectedComplaint));

        JsonPostComplaint actualComplaint = complaintManager.getJsonPostComplaint(person.getPersonId(), post, 1L);
        assertEquals(expectedComplaint.getReason().getId(), actualComplaint.getReason().getId());
        assertEquals(expectedComplaint.getAuthorId(), actualComplaint.getAuthorId());
        assertEquals(expectedComplaint.getState(), actualComplaint.getState());
        assertEquals(expectedComplaint.getPost().getId(), actualComplaint.getPostId());
    }

    @Test
    void findComplaints_withoutPermissions_throwException() {
        PersonDTO person = getPerson(2L);
        Post post = getPost(1L, 2L, PostState.PUBLISHED);
        Pageable unPaged = Pageable.unpaged();

        assertThatThrownBy(() -> complaintManager.findComplaints(person, post, unPaged))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.operation.not.permitted");

    }

    @Test
    void findComplaints_withPermissions_returnPaginatedResult() {
        PersonDTO person = getPerson(1L);
        person.setUuid(UUID.randomUUID());

        Post post = getPost(1L, 2L, PostState.PUBLISHED);
        Reason reason = mapper.map(getReason(), Reason.class);

        PostComplaint expectedComplaint = PostComplaint.builder()
                .id(1L)
                .post(post)
                .authorId(person.getPersonId())
                .reason(reason)
                .state(PostComplaintState.PENDING)
                .build();

        PersonDTO personDTO = PersonDTO.builder()
                .personId(3L)
                .displayName("Name")
                .build();

        Pageable pageable = PageRequest.of(0, 3);
        List<PostComplaint> complaintList = Collections.singletonList(expectedComplaint);
        Page<PostComplaint> expectedPage = new PageImpl<>(complaintList, pageable, complaintList.size());

        given(permissionUtil.hasGroupPermissions(person, post.getUserGroupId())).willReturn(true);
        given(complaintRepository.findComplaints(post.getId(), pageable)).willReturn(expectedPage);
        given(personManager.getPersonsByUuids(any())).willReturn(Collections.singletonList(personDTO));

        Page<JsonPostComplaint> actualPage = complaintManager.findComplaints(person, post, pageable);

        assertEquals(expectedPage.getTotalElements(), actualPage.getTotalElements());
        assertEquals(expectedPage.getContent().get(0).getAuthorId(), actualPage.getContent().get(0).getAuthorId());
    }

    @Test
    void updateComplaint_notFound_throwException() {
        PersonDTO person = getPerson(2L);
        Post post = getPost(1L, 2L, PostState.PUBLISHED);
        JsonPostComplaintData data = JsonPostComplaintData.builder()
                .reason(getReason())
                .build();

        given(complaintRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> complaintManager.updateComplaint(person.getPersonId(), post, 1L, data))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining(ERROR_NOT_FOUND_COMPLAINT);
    }

    @Test
    void updateComplaint_found_returnUpdatedComplaint() {
        PersonDTO person = getPerson(2L);
        person.setUuid(UUID.randomUUID());

        Post post = getPost(1L, 2L, PostState.PUBLISHED);
        Reason reason = mapper.map(getReason(), Reason.class);
        PostComplaint expectedComplaint = PostComplaint.builder()
                .id(1L)
                .post(post)
                .authorId(person.getPersonId())
                .reason(reason)
                .state(PostComplaintState.POST_REJECTED)
                .build();
        PostComplaintData data = JsonPostComplaintData.builder()
                .reason(getReason())
                .build();
        given(complaintRepository.findById(1L)).willReturn(Optional.of(expectedComplaint));
        given(reasonManager.getComplaintReason(reason.getId())).willReturn(reason);
        given(personManager.getPersonsByIds(any())).willReturn(Collections.singletonList(person));

        JsonPostComplaint actualComplaint = complaintManager.updateComplaint(person.getPersonId(), post, 1L, data);

        assertEquals(expectedComplaint.getReason().getId(), actualComplaint.getReason().getId());
        assertEquals(expectedComplaint.getAuthorId(), actualComplaint.getAuthorId());
        assertEquals(expectedComplaint.getState(), actualComplaint.getState());
        assertEquals(expectedComplaint.getPost().getId(), actualComplaint.getPostId());
    }

    @Test
    void removeComplaint_notFound_throwException() {
        PersonDTO person = getPerson(2L);
        Post post = getPost(1L, 2L, PostState.PUBLISHED);

        given(complaintRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> complaintManager.removeComplaint(person.getPersonId(), post, 1L))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining(ERROR_NOT_FOUND_COMPLAINT);
    }

    @Test
    void removeComplaint_found_noErrors() {
        PersonDTO person = getPerson(2L);
        Post post = getPost(1L, 2L, PostState.PUBLISHED);
        PostComplaint toDelete = PostComplaint.builder()
                .id(1L)
                .post(post)
                .authorId(person.getPersonId())
                .state(PostComplaintState.POST_REJECTED)
                .build();
        given(complaintRepository.findById(1L)).willReturn(Optional.of(toDelete));

        complaintManager.removeComplaint(person.getPersonId(), post, 1L);

        verify(complaintRepository).delete(toDelete);
    }

    @Test
    void rejectAllComplaints_postNotFound_throwException() {
        PersonDTO person = getPerson(2L);
        Long postId = 1L;

        given(postManager.getPost(postId)).willThrow(new NotFoundException("error.not.found.post", postId));

        assertThatThrownBy(() -> complaintManager.rejectAllComplaints(person, REJECT_REASON, postId))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.not.found.post");
    }

    @Test
    void rejectAllComplaints_withoutPermissions_throwException() {
        PersonDTO person = getPerson(2L);
        Long postId = 1L;
        Post post = getPost(postId, 2L, PostState.PUBLISHED);

        given(postManager.getPost(postId)).willReturn(post);

        assertThatThrownBy(() -> complaintManager.rejectAllComplaints(person, REJECT_REASON, postId))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.operation.not.permitted");
    }

    @Test
    void rejectAllComplaints_updateState_noErrors() {
        PersonDTO person = getPerson(2L);
        Post post = getPost(1L, 2L, PostState.PUBLISHED);

        PostComplaint expectedComplaint = PostComplaint.builder()
                .id(1L)
                .post(post)
                .authorId(person.getPersonId())
                .reason(Reason.builder().name(REJECT_REASON).build())
                .state(PostComplaintState.REJECTED)
                .build();

        Pageable pageable = PageRequest.of(0, 3);
        List<PostComplaint> complaintList = Collections.singletonList(expectedComplaint);
        Page<PostComplaint> expectedPage = new PageImpl<>(complaintList, pageable, complaintList.size());

        given(complaintRepository.findByPostIdAndState(post.getId(), PostComplaintState.PENDING, Pageable.unpaged())).willReturn(expectedPage);
        given(permissionUtil.hasGroupPermissions(person, post.getUserGroupId())).willReturn(true);
        given(postManager.getPost(post.getId())).willReturn(post);

        complaintManager.rejectAllComplaints(person, REJECT_REASON, post.getId());

        verify(complaintRepository).save(expectedPage.getContent().get(0));

    }

    private Post getPost(Long postId, Long personId, PostState state) {
        return Post.builder()
                .id(postId)
                .authorId(personId)
                .userGroupId(1L)
                .state(state)
                .build();
    }

    private PersonDTO getPerson(Long personId) {
        return PersonDTO.builder()
                .personId(personId)
                .build();
    }

    private JsonReason getReason() {
        JsonReason reason = new JsonReason();
        reason.setId(1L);
        reason.setName("test reason");
        return reason;
    }
}
