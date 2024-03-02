package iq.earthlink.social.postservice.service;

import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.rest.JsonMemberPermission;
import iq.earthlink.social.groupservice.group.rest.MembersRestService;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CollectionPostsSearchCriteria;
import iq.earthlink.social.postservice.post.collection.DefaultGroupPostCollectionService;
import iq.earthlink.social.postservice.post.collection.GroupPostCollection;
import iq.earthlink.social.postservice.post.collection.repository.GroupPostCollectionRepository;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.JsonGroupPostCollectionData;
import iq.earthlink.social.postservice.util.PermissionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class DefaultGroupPostCollectionServiceTest {

    private static final String ADMIN = "ADMIN";
    @InjectMocks
    private DefaultGroupPostCollectionService service;

    @Mock
    private GroupPostCollectionRepository repository;

    @Mock
    private MembersRestService membersRestService;

    @Mock
    private PermissionUtil permissionUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void createGroupCollection_validData_success() {
        // given
        Long personId = 1L;
        Long groupId = 2L;
        PersonDTO person = PersonDTO
                .builder()
                .personId(personId)
                .build();

        var data = new JsonGroupPostCollectionData();
        data.setGroupId(groupId);
        data.setName("name");
        data.setDefaultCollection(true);

        // when
        when(permissionUtil.hasGroupPermissions(person, groupId)).thenReturn(true);
        when(repository.save(any(GroupPostCollection.class))).thenReturn(new GroupPostCollection());

        // then
        assertNotNull(service.createGroupCollection(person, data));
    }

    @Test
    void createGroupCollection_invalidData_throwNullPointerException() {
        // given
        Long personId = 1L;
        Long groupId = 2L;
        PersonDTO person = PersonDTO
                .builder()
                .personId(personId)
                .build();

        JsonGroupPostCollectionData data = null;
        var permission = JsonMemberPermission
                .builder()
                .groupId(groupId)
                .personId(personId)
                .statuses(List.of(GroupMemberStatus.ADMIN))
                .build();
        // when
        when(membersRestService.getMember(anyLong(), anyLong())).thenReturn(permission);
        when(repository.save(any(GroupPostCollection.class))).thenReturn(new GroupPostCollection());

        // then
        assertThrows(NullPointerException.class, () -> service.createGroupCollection(person, data));
    }

    @Test
    void createGroupCollection_personDoesNotHavePermission_throwRestApiException() {
        // given
        Long personId = 1L;
        Long groupId = 2L;
        PersonDTO person = PersonDTO
                .builder()
                .personId(personId)
                .build();

        var data = new JsonGroupPostCollectionData();
        data.setGroupId(groupId);
        data.setName("name");
        data.setDefaultCollection(true);

        // when
        when(membersRestService.getMember(data.getGroupId(), person.getPersonId())).thenThrow(new RestApiException(HttpStatus.FORBIDDEN, "error.person.can.not.modify.group.collection"));
        when(repository.save(any(GroupPostCollection.class))).thenReturn(new GroupPostCollection());

        // then
        assertThrows(RestApiException.class, () -> service.createGroupCollection(person, data));
    }

    @Test
    void getGroupCollection_byIdExist_success() {
        // given
        Long groupCollectionId = 1L;
        GroupPostCollection groupPostCollection = GroupPostCollection
                .builder()
                .id(groupCollectionId)
                .build();

        // when
        when(repository.findById(groupCollectionId)).thenReturn(Optional.of(groupPostCollection));

        // then
        var result = service.getGroupCollection(groupCollectionId);
        assertNotNull(result);
        assertEquals(groupPostCollection, result);
        assertEquals(groupCollectionId, result.getId());
    }

    @Test
    void getGroupCollection_byIdNotExist_throwNotFoundException() {
        // given
        Long groupCollectionId = 1L;

        // when
        when(repository.findById(groupCollectionId)).thenReturn(Optional.empty());

        // then
        assertThrows(NotFoundException.class, () -> service.getGroupCollection(groupCollectionId));
    }

    @Test
    void removeGroupCollection_byIdExist_success() {
        // given
        Long groupCollectionId = 1L;
        Long personId = 2L;
        Long groupId=3L;
        GroupPostCollection groupPostCollection = GroupPostCollection
                .builder()
                .id(groupCollectionId)
                .groupId(groupId)
                .build();
        PersonDTO person = PersonDTO
                .builder()
                .personId(personId)
                .roles(Set.of(ADMIN))
                .build();

        when(permissionUtil.hasGroupPermissions(person, groupId)).thenReturn(true);
        when(repository.findById(groupCollectionId)).thenReturn(Optional.of(groupPostCollection));
        // when
        service.removeGroupCollection(person, groupCollectionId);

        // then
        verify(repository, times(1)).delete(any());
    }

    @Test
    void removeGroupCollection_byIdNotExist_throwNotFoundException() {
        // given
        Long groupCollectionId = 1L;
        Long personId = 2L;
        PersonDTO person = PersonDTO
                .builder()
                .personId(personId)
                .roles(Set.of(ADMIN))
                .build();

        // when
        when(repository.findById(groupCollectionId)).thenReturn(Optional.empty());

        // then
        assertThrows(NotFoundException.class, () -> service.removeGroupCollection(person, groupCollectionId));
    }

    @Test
    void getGroupCollectionPosts_byIdExist_success() {
        // given
        Long personId = 1L;
        Long groupCollectionId = 1L;
        GroupPostCollection groupPostCollection = GroupPostCollection
                .builder()
                .id(groupCollectionId)
                .build();
        CollectionPostsSearchCriteria criteria = CollectionPostsSearchCriteria
                .builder()
                .build();
        // when
        when(repository.findPosts(groupCollectionId, criteria, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(new Post())));
        when(repository.findById(groupCollectionId)).thenReturn(Optional.of(groupPostCollection));

        // then
        var result = service.getGroupCollectionPosts(groupCollectionId, criteria, Pageable.unpaged());
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getGroupCollectionPosts_byIdNotExist_returnEmptyList() {
        // given
        Long personId = 1L;
        Long groupCollectionId = 1L;
        CollectionPostsSearchCriteria criteria = CollectionPostsSearchCriteria
                .builder()
                .build();
        // when
        when(repository.findPosts(groupCollectionId, criteria, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of()));
        when(repository.findById(groupCollectionId)).thenReturn(Optional.empty());

        // then
        var result = service.getGroupCollectionPosts(groupCollectionId, criteria, Pageable.unpaged());
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());

    }

    @Test
    void addPost_success() {
        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .roles(Set.of(ADMIN))
                .build();
        Post post = new Post();
        Long groupCollectionId = 1L;
        Long groupId = 3L;
        GroupPostCollection c = new GroupPostCollection();
        c.setId(groupCollectionId);
        c.setGroupId(groupId);
        c.setPosts(new HashSet<>());

        when(repository.findById(groupCollectionId)).thenReturn(Optional.of(c));
        when(permissionUtil.hasGroupPermissions(person, groupId)).thenReturn(true);

        service.addPost(person, post, groupCollectionId);

        assertEquals(1, c.getPosts().size());
    }

    @Test
    void removePost_success() {
        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .roles(Set.of(ADMIN))
                .build();
        Post post = new Post();
        Long groupCollectionId = 1L;

        GroupPostCollection c = new GroupPostCollection();
        c.setId(groupCollectionId);
        c.setGroupId(1L);
        c.setPosts(new HashSet<>());
        c.getPosts().add(post);

        when(repository.findById(groupCollectionId)).thenReturn(Optional.of(c));
        when(permissionUtil.hasGroupPermissions(person, 1L)).thenReturn(true);

        service.removePost(person, post, groupCollectionId);

        assertEquals(0, c.getPosts().size());
    }

    @Test
    void addPost_notAuthorized() {
        PersonDTO person = PersonDTO.builder().personId(1L).build();
        Post post = new Post();
        Long groupCollectionId = 1L;

        when(repository.findById(groupCollectionId)).thenReturn(Optional.of(new GroupPostCollection()));
        when(membersRestService.getMember(groupCollectionId, person.getPersonId())).thenReturn(null);

        assertThrows(ForbiddenException.class, () -> service.addPost(person, post, groupCollectionId));
    }

    @Test
    void removePost_notAuthorized() {
        PersonDTO person = PersonDTO.builder().personId(1L).build();
        Post post = new Post();
        Long groupCollectionId = 1L;

        when(repository.findById(groupCollectionId)).thenReturn(Optional.of(new GroupPostCollection()));
        when(membersRestService.getMember(groupCollectionId, person.getPersonId())).thenReturn(null);

        assertThrows(ForbiddenException.class, () -> service.removePost(person, post, groupCollectionId));
    }

}
