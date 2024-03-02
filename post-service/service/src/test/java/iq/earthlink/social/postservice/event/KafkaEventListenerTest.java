package iq.earthlink.social.postservice.event;

import iq.earthlink.social.classes.data.dto.ContentModerationDto;
import iq.earthlink.social.classes.enumeration.ContentType;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.postservice.post.DefaultPostManager;
import iq.earthlink.social.postservice.post.comment.Comment;
import iq.earthlink.social.postservice.post.comment.DefaultCommentService;
import iq.earthlink.social.postservice.post.comment.repository.CommentRepository;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class KafkaEventListenerTest {

    @Mock
    private PostRepository repository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    @Spy
    private KafkaEventListener eventListener;

    @Mock
    private DefaultPostManager postManager;

    @InjectMocks
    private DefaultPostManager postManagerInjected;

    @Mock
    private DefaultCommentService commentManager;

    @InjectMocks
    private DefaultCommentService commentManagerInjected;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void deleteModeratedContent_deleteOffensivePost_postDeleted() {
        //given
        ContentModerationDto dto = ContentModerationDto.builder().type(ContentType.POST.getDisplayName()).id(10L).reasonKey("0000").build();

        Post post = Post.builder().id(dto.getId()).build();
        Comment comment = Comment.builder().id(1L).post(post).build();
        when(repository.findById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.findByPostId(post.getId(), Pageable.unpaged())).thenReturn(new PageImpl<>(Collections.singletonList(comment), Pageable.unpaged(), 1));

        //when
        eventListener.deleteModeratedContent(dto);

        //then
        verify(postManager, times(1)).removePostByModerator(any());
        when(repository.findById(post.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postManagerInjected.removePostByModerator(dto)).isInstanceOf(RestApiException.class).hasMessageContaining("error.not.found.post");

    }

    @Test
    void deleteModeratedContent_deleteOffensiveComment_commentDeleted() {
        //given
        ContentModerationDto dto = ContentModerationDto.builder().type(ContentType.POST_COMMENT.getDisplayName()).id(1L).reasonKey("0000").build();
        Post post = Post.builder().id(10L).build();
        Comment comment = Comment.builder().id(1L).post(post).build();

        when(commentRepository.findById(dto.getId())).thenReturn(Optional.of(comment));

        //when
        eventListener.deleteModeratedContent(dto);

        //then
        verify(commentManager, times(1)).removeCommentByModerator(any());
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentManagerInjected.removeCommentByModerator(dto)).isInstanceOf(RestApiException.class).hasMessageContaining("error.not.found.comment");
    }

}