package iq.earthlink.social.feedaggregatorservice.service;

import iq.earthlink.social.feedaggregatorservice.dto.PostDTO;
import iq.earthlink.social.feedaggregatorservice.dto.RecentPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class RecentFeedAggregatorServiceImplTest {

    @Mock
    private RedisTemplate<String, RecentPost> redisTemplate;

    @Mock
    private ListOperations<String, RecentPost> listOperations;

    private RecentFeedAggregatorServiceImpl feedAggregatorService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        feedAggregatorService = new RecentFeedAggregatorServiceImpl(redisTemplate);
    }

    @Test
    void getFeedByGroups_getFromGroupsThatNotExistInRedis_returnEmptyList() {
        //given
        List<Long> groupIds = List.of(1L, 2L);
        List<PostDTO> expected = new ArrayList<>();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("group:1:posts", 0, -1)).thenReturn(null);
        when(listOperations.range("group:2:posts", 0, -1)).thenReturn(null);

        //when
        List<PostDTO> result = feedAggregatorService.getFeedByGroups(groupIds);

        //then
        assertEquals(expected, result);
    }

    @Test
    void getFeedByGroups_getFromThreeGroups_returnMergedRecentPostsFromAllGroups() {
        //given
        List<Long> groupIds = List.of(1L, 2L, 3L);
        List<RecentPost> recentPostsGroup1 = new ArrayList<>();
        recentPostsGroup1.add(new RecentPost("uuid3", 1690000006L, 1L));
        recentPostsGroup1.add(new RecentPost("uuid2", 1690000002L, 1L));
        recentPostsGroup1.add(new RecentPost("uuid1", 1690000000L, 1L));

        List<RecentPost> recentPostsGroup2 = new ArrayList<>();
        recentPostsGroup2.add(new RecentPost("uuid6", 1690000008L, 2L));
        recentPostsGroup2.add(new RecentPost("uuid5", 1690000003L, 2L));
        recentPostsGroup2.add(new RecentPost("uuid4", 1690000001L, 2L));

        List<RecentPost> recentPostsGroup3 = new ArrayList<>();
        recentPostsGroup3.add(new RecentPost("uuid7", 1690000005L, 3L));

        List<PostDTO> expected = new ArrayList<>();
        expected.add(new PostDTO("uuid6", 2L));
        expected.add(new PostDTO("uuid3", 1L));
        expected.add(new PostDTO("uuid7", 3L));
        expected.add(new PostDTO("uuid5", 2L));
        expected.add(new PostDTO("uuid2", 1L));
        expected.add(new PostDTO("uuid4", 2L));
        expected.add(new PostDTO("uuid1", 1L));

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("group:1:posts", 0, -1)).thenReturn(recentPostsGroup1);
        when(listOperations.range("group:2:posts", 0, -1)).thenReturn(recentPostsGroup2);
        when(listOperations.range("group:3:posts", 0, -1)).thenReturn(recentPostsGroup3);

        //when
        List<PostDTO> result = feedAggregatorService.getFeedByGroups(groupIds);

        //then
        assertEquals(expected, result);
    }
}