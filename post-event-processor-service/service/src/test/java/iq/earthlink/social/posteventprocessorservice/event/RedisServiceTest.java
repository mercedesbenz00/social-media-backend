package iq.earthlink.social.posteventprocessorservice.event;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import iq.earthlink.social.posteventprocessorservice.dto.PostEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisServiceTest {

    @Mock
    private UnifiedJedis jedis;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Counter counter;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void sendToRedis_provideValidData_sendToRedis() {
        PostEvent postEvent = new PostEvent();
        postEvent.setPostUuid("4c7746ea-3f1f-4133-8f74-51449c2a97ba");
        postEvent.setUserGroupId(1L);
        Long eventWeight = 10L;
        when(meterRegistry.counter("post.event.send.to.redis.count")).thenReturn(counter);

        redisService.sendToRedis(postEvent, eventWeight);

        verify(jedis, times(1)).topkIncrBy(anyString(), anyMap());
        verify(jedis, times(0)).topkReserve(anyString(), anyInt(), anyInt(), anyInt(), anyDouble());
        verify(counter, times(1)).increment();
    }

    @Test
    void sendToRedis_thisTopKNotExist_topKCreatedValueAdded() {
        PostEvent postEvent = new PostEvent();
        postEvent.setPostUuid("4c7746ea-3f1f-4133-8f74-51449c2a97ba");
        postEvent.setUserGroupId(1L);
        Long eventWeight = 10L;

        when(meterRegistry.counter("post.event.send.to.redis.count")).thenReturn(counter);
        when(jedis.topkIncrBy(any(), any()))
                .thenThrow(new JedisDataException("Redis error"))
                .thenReturn(new ArrayList<>());

        redisService.sendToRedis(postEvent, eventWeight);

        verify(jedis, times(1)).topkReserve(anyString(), anyLong(), anyLong(), anyLong(), anyDouble());
        verify(jedis, times(2)).topkIncrBy(anyString(), anyMap());
        verify(counter, times(1)).increment();
    }

    @Test
    void testSendToRedis_errorWhenHandleRedisError_throwJedisDataException() {
        PostEvent postEvent = new PostEvent();
        postEvent.setPostUuid("4c7746ea-3f1f-4133-8f74-51449c2a97ba");
        postEvent.setUserGroupId(1L);
        Long eventWeight = 10L;

        when(meterRegistry.counter("post.event.send.to.redis.error.count")).thenReturn(counter);
        when(jedis.topkIncrBy(any(), any())).thenThrow(new JedisDataException("Redis error"));

        assertThrows(JedisDataException.class, () -> redisService.sendToRedis(postEvent, eventWeight));

        verify(jedis, times(1)).topkReserve(anyString(), anyLong(), anyLong(), anyLong(), anyDouble());
        verify(jedis, times(2)).topkIncrBy(anyString(), anyMap());
        verify(counter, times(1)).increment();
    }
}


