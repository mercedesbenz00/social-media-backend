package iq.earthlink.social.feedaggregatorservice.service;

import iq.earthlink.social.feedaggregatorservice.dto.PostDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class TopFeedAggregatorServiceImplTest {

    @Mock
    private UnifiedJedis jedis;

    private TopFeedAggregatorServiceImpl feedAggregatorService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        feedAggregatorService = new TopFeedAggregatorServiceImpl(jedis);
    }

    @Test
    void getFeedByGroups_getFromGroupsThatNotExistInRedis_returnEmptyList() {
        //given
        List<Long> groupIds = List.of(1L, 2L);
        List<PostDTO> expected = new ArrayList<>();

        when(jedis.topkList("1_group_topk")).thenThrow(new JedisDataException(""));
        when(jedis.topkList("2_group_topk")).thenThrow(new JedisDataException(""));

        //when
        List<PostDTO> result = feedAggregatorService.getFeedByGroups(groupIds);

        //then
        assertEquals(expected, result);
    }

    @Test
    void getFeedByGroups_getFromThreeGroups_returnTopCombinedPostsFromAllGroups() {
        //given
        List<Long> groupIds = List.of(1L, 2L, 3L);
        List<String> topPostUuidsGroup1 = List.of("uuid1", "uuid2");
        List<String> topPostUuidsGroup2 = List.of("uuid3", "uuid4", "uuid5");
        List<String> topPostUuidsGroup3 = List.of("uuid6", "uuid7");

        List<PostDTO> expected = new ArrayList<>();
        expected.add(new PostDTO("uuid1", 1L));
        expected.add(new PostDTO("uuid3", 2L));
        expected.add(new PostDTO("uuid6", 3L));
        expected.add(new PostDTO("uuid2", 1L));
        expected.add(new PostDTO("uuid4", 2L));
        expected.add(new PostDTO("uuid7", 3L));
        expected.add(new PostDTO("uuid5", 2L));

        when(jedis.topkList("1_group_topk")).thenReturn(topPostUuidsGroup1);
        when(jedis.topkList("2_group_topk")).thenReturn(topPostUuidsGroup2);
        when(jedis.topkList("3_group_topk")).thenReturn(topPostUuidsGroup3);

        //when
        List<PostDTO> result = feedAggregatorService.getFeedByGroups(groupIds);

        //then
        assertEquals(expected, result);
    }
}