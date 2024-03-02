package iq.earthlink.social.posteventprocessorservice.event;

import io.micrometer.core.instrument.MeterRegistry;
import iq.earthlink.social.posteventprocessorservice.dto.PostEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class RedisService {
    private final UnifiedJedis jedis;
    private final MeterRegistry meterRegistry;

    @Autowired
    public RedisService(UnifiedJedis jedis, MeterRegistry meterRegistry) {
        this.jedis = jedis;
        this.meterRegistry = meterRegistry;
    }

    public void sendToRedis(PostEvent postEvent, Long eventWeight) {
        String topkListName = postEvent.getUserGroupId().toString() + "_group_topk";

        HashMap<String, Long> hashMap = new HashMap<>();
        hashMap.put(postEvent.getPostUuid(), eventWeight);
        try {
            jedis.topkIncrBy(topkListName, hashMap);
        } catch (JedisDataException ex) {
            handleRedisError(topkListName, hashMap);
        }
        meterRegistry.counter("post.event.send.to.redis.count").increment();
    }

    private void handleRedisError(String topkListName, Map<String, Long> hashMap) {
        try {
            jedis.topkReserve(topkListName, 1000, 3000, 5, 0.9);
            jedis.topkIncrBy(topkListName, hashMap);
        } catch (Exception ex) {
            log.error("Failed to handle Redis error: {}", ex.getMessage());
            meterRegistry.counter("post.event.send.to.redis.error.count").increment();
            throw ex;
        }
    }
}
