package iq.earthlink.social.userfeedaggregatorservice.service;

import iq.earthlink.social.postservice.rest.PostRestService;
import iq.earthlink.social.security.SecurityProvider;
import iq.earthlink.social.userfeedaggregatorservice.dto.FeedAggregatorResponse;
import iq.earthlink.social.userfeedaggregatorservice.dto.FeedRequest;
import iq.earthlink.social.userfeedaggregatorservice.dto.FeedResponse;
import iq.earthlink.social.userfeedaggregatorservice.feign.FeedAggregatorRestService;
import iq.earthlink.social.userfeedaggregatorservice.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.naming.ServiceUnavailableException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;


class UserFeedServiceImplTest {

    @Mock
    private FeedAggregatorRestService feedAggregatorRestService;

    @Mock
    private Utils timestampUtils;

    @Mock
    private PostRestService postRestService;

    @Mock
    private UserFeedCache feedCache;

    @Mock
    private SecurityProvider securityProvider;

    @InjectMocks
    private UserFeedServiceImpl userFeedService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetUserFeedFirstTime() throws ServiceUnavailableException {
        // given
        String authorizationHeader = "Bearer some_token";
        Long personId = 123L;
        List<Long> groupIds = List.of(1L, 2L);
        FeedRequest feedRequest = FeedRequest.builder()
                .page(0)
                .size(10)
                .build();
        given(securityProvider.getPersonIdFromAuthorization(anyString())).willReturn(personId);
        given(feedCache.getTimestamp("recent", personId)).willReturn(null);
        given(timestampUtils.getTimestamp()).willReturn(Instant.now().toEpochMilli());
        List<FeedAggregatorResponse> feedAggregatorResponses = new ArrayList<>();
        given(feedAggregatorRestService.getPosts(anyString(), eq(groupIds))).willReturn(feedAggregatorResponses);
        given(postRestService.getPosts(anyString(), anyList())).willReturn(new ArrayList<>());

        // when
        FeedResponse result = userFeedService.getUserFeed(authorizationHeader, feedRequest);

        // then
        assertNotNull(result);
        assertEquals(0, result.getNumber());
        assertEquals(0, result.getContent().size());
    }

    @Test
    void testGetUserFeedCachedData() throws ServiceUnavailableException {
        // given
        String authorizationHeader = "Bearer some_token";
        Long personId = 123L;
        Long timestamp = Instant.now().toEpochMilli();
        List<Long> groupIds = List.of(1L, 2L);
        FeedRequest feedRequest = FeedRequest.builder()
                .size(10)
                .timestamp(timestamp)
                .page(0)
                .build();
        given(securityProvider.getPersonIdFromAuthorization(anyString())).willReturn(personId);
        given(feedCache.getTimestamp("recent", personId)).willReturn(timestamp);
        given(timestampUtils.getTimestamp()).willReturn(Instant.now().toEpochMilli());
        List<FeedAggregatorResponse> feedAggregatorResponses = new ArrayList<>();
        given(feedAggregatorRestService.getPosts(anyString(), eq(groupIds))).willReturn(feedAggregatorResponses);
        given(postRestService.getPosts(anyString(), anyList())).willReturn(new ArrayList<>());
        given(timestampUtils.isTimestampExpired(eq(timestamp), anyInt())).willReturn(false);

        // when
        FeedResponse result = userFeedService.getUserFeed(authorizationHeader, feedRequest);

        // then
        assertNotNull(result);
        assertEquals(0, result.getNumber());
        assertEquals(0, result.getContent().size());
    }

    @Test
    void testGetUserFeedCachedDataExpired() throws ServiceUnavailableException {
        // given
        String authorizationHeader = "Bearer some_token";
        Long personId = 123L;
        Long timestamp = Instant.now().minusSeconds(300).toEpochMilli(); // Expired timestamp
        List<Long> groupIds = List.of(1L, 2L);
        FeedRequest feedRequest = FeedRequest.builder()
                .size(10)
                .timestamp(timestamp)
                .page(0)
                .build();
        given(securityProvider.getPersonIdFromAuthorization(anyString())).willReturn(personId);
        given(feedCache.getTimestamp("recent", personId)).willReturn(timestamp);
        given(timestampUtils.getTimestamp()).willReturn(Instant.now().toEpochMilli());
        List<FeedAggregatorResponse> feedAggregatorResponses = new ArrayList<>();
        given(feedAggregatorRestService.getPosts(anyString(), eq(groupIds))).willReturn(feedAggregatorResponses);
        given(postRestService.getPosts(anyString(), anyList())).willReturn(new ArrayList<>());
        given(timestampUtils.isTimestampExpired(eq(timestamp), anyInt())).willReturn(true);

        // when
        FeedResponse result = userFeedService.getUserFeed(authorizationHeader, feedRequest);

        // then
        assertNotNull(result);
        assertEquals(0, result.getNumber());
        assertEquals(0, result.getContent().size());
    }
}
