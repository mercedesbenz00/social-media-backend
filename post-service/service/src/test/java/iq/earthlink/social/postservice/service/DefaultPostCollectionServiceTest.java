package iq.earthlink.social.postservice.service;

import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.CollectionPostsSearchCriteria;
import iq.earthlink.social.postservice.post.collection.DefaultPostCollectionService;
import iq.earthlink.social.postservice.post.collection.PostCollection;
import iq.earthlink.social.postservice.post.collection.repository.PostCollectionPostsRepository;
import iq.earthlink.social.postservice.post.collection.repository.PostCollectionRepository;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.rest.JsonPost;
import iq.earthlink.social.postservice.post.rest.JsonPostCollection;
import iq.earthlink.social.postservice.post.rest.JsonPostCollectionData;
import iq.earthlink.social.postservice.util.PostStatisticsAndMediaUtil;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.jetbrains.annotations.NotNull;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.AdditionalAnswers.returnsLastArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultPostCollectionServiceTest {
    @InjectMocks
    private DefaultPostCollectionService postCollectionService;
    @Mock
    private PostCollectionRepository repository;
    @Mock
    private PostCollectionPostsRepository postCollectionPostsRepository;
    @Mock
    private PostStatisticsAndMediaUtil postStatisticsAndMediaUtil;
    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void createCollection_createCollectionWithValidData_returnPostCollection() {
        //given
        Long personId = 1L;

        JsonPostCollectionData postCollectionData = new JsonPostCollectionData();
        postCollectionData.setIsPublic(true);
        postCollectionData.setName("collection name");

        given(repository.save(any())).will(returnsFirstArg());

        //when
        PostCollection collection = postCollectionService.createCollection(personId, postCollectionData);

        //then
        assertEquals(personId, collection.getOwnerId());
        assertEquals(postCollectionData.getName(), collection.getName());
        assertEquals(postCollectionData.getIsPublic(), collection.isPublic());
    }

    @Test
    void getCollection_getCollectionById_returnPostCollection() {
        //given
        PersonInfo person = getPersonProfile();
        PostCollection postCollection = PostCollection.builder()
                .id(1L)
                .isPublic(true)
                .name("Name")
                .ownerId(1L)
                .build();

        given(repository.findById(any())).willReturn(Optional.of(postCollection));

        //when
        PostCollection collection = postCollectionService.getCollection(person.getId(), 1L);

        //then
        assertEquals(postCollection.getOwnerId(), collection.getOwnerId());
        assertEquals(postCollection.getName(), collection.getName());
        assertEquals(postCollection.isPublic(), collection.isPublic());
    }

    @Test
    void addPost_addPostToPostCollection_postAddedToPostCollection() {
        //given
        PersonInfo person = getPersonProfile();

        Post post = Post.builder()
                .id(1L)
                .state(PostState.PUBLISHED)
                .build();

        PostCollection postCollection = PostCollection.builder()
                .id(1L)
                .isPublic(true)
                .name("Name")
                .ownerId(1L)
                .build();

        given(repository.findById(any())).willReturn(Optional.of(postCollection));

        //when
        postCollectionService.addPost(person.getId(), post, postCollection.getId());

        //then
        verify(postCollectionPostsRepository, times(1)).save(any());
    }

    @Test
    void removePost_removePostFromPostCollection_returnPostCollection() {
        //given
        PersonInfo person = getPersonProfile();

        Post post = Post.builder()
                .id(1L)
                .state(PostState.PUBLISHED)
                .build();

        PostCollection postCollection = PostCollection.builder()
                .id(1L)
                .isPublic(true)
                .name("Name")
                .ownerId(1L)
                .build();

        given(repository.findById(any())).willReturn(Optional.of(postCollection));

        //when
        postCollectionService.removePost(person.getId(), post, postCollection.getId());

        //then
        verify(postCollectionPostsRepository, times(1))
                .deleteByPostCollectionIdAndPostId(postCollection.getId(), post.getId());
    }

    @Test
    void getCollectionPosts() {
        //given
        PersonInfo person = getPersonProfile();
        HashSet<Post> posts = getPosts();

        long postCollectionId = 1L;
        PostCollection postCollection = PostCollection.builder()
                .id(postCollectionId)
                .isPublic(true)
                .name("Name")
                .ownerId(1L)
                .build();

        CollectionPostsSearchCriteria criteria = CollectionPostsSearchCriteria.builder()
                .query(null)
                .build();

        Pageable pageable = PageRequest.of(0, 3);
        Page<Post> page = new PageImpl<>(new ArrayList<>(posts), PageRequest.of(0, 3), posts.size());

        given(postCollectionPostsRepository.findPosts(any(), any(), any())).willReturn(page);
        given(repository.findById(any())).willReturn(Optional.ofNullable(postCollection));
        given(postStatisticsAndMediaUtil.enrichWithStatisticsAndMedia(any(PersonInfo.class), any())).will(returnsLastArg());

        //when
        Page<JsonPost> collectionPosts = postCollectionService.getCollectionPosts(person.getId(), postCollectionId, criteria, pageable);

        //then
        assertEquals(2, collectionPosts.getContent().size());
    }

    @Test
    void removeCollection() {
        //given
        PersonDTO person = PersonDTO.builder()
                .personId(1L)
                .build();;

        long postCollectionId = 1L;
        PostCollection postCollection = PostCollection.builder()
                .id(postCollectionId)
                .isPublic(true)
                .name("Name")
                .ownerId(1L)
                .build();

        given(repository.findById(any())).willReturn(Optional.ofNullable(postCollection));

        //when
        postCollectionService.removeCollection(person, postCollectionId);

        //then
        verify(repository, times(1)).delete(any());
        verify(postCollectionPostsRepository, times(1)).deleteByPostCollectionId(postCollectionId);
    }

    @Test
    void findMyCollectionByPostId_findByExistingPostId_returnPostCollection() {
        //given
        PersonInfo person = getPersonProfile();
        Long postId = 1L;
        PostCollection postCollection = PostCollection.builder()
                .id(1L)
                .isPublic(true)
                .name("Name")
                .ownerId(1L)
                .build();

        given(postCollectionPostsRepository.findCollectionIdByPostId(any())).willReturn(List.of(postId));
        given(repository.findByIdInAndOwnerId(any(), any())).willReturn(Optional.of(postCollection));

        //when
        JsonPostCollection collection = postCollectionService.findMyCollectionByPostId(person.getId(), postId);

        //then
        assertEquals(postCollection.getOwnerId(), collection.getOwnerId());
        assertEquals(postCollection.getName(), collection.getName());
        assertEquals(postCollection.isPublic(), collection.isPublic());
    }

    @Test
    void findMyCollectionByPostId_findByNotExistingPostId_throwNotFoundException() {
        //given
        Long personId = 1L;
        Long postId = 1L;

        given(repository.findById(any())).willReturn(Optional.empty());

        //when
        //then
        assertThatThrownBy(() -> postCollectionService.findMyCollectionByPostId(personId, postId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("error.not.found.collection.by.post.id");
    }

    private PersonInfo getPersonProfile() {
        return JsonPersonProfile.builder()
                .id(1L)
                .username("userName")
                .build();
    }

    @NotNull
    private HashSet<Post> getPosts() {
        Post post1 = Post.builder()
                .id(1L)
                .state(PostState.PUBLISHED)
                .build();

        Post post2 = Post.builder()
                .id(2L)
                .state(PostState.PUBLISHED)
                .build();

        HashSet<Post> posts = new HashSet<>();
        posts.add(post1);
        posts.add(post2);
        return posts;
    }
}