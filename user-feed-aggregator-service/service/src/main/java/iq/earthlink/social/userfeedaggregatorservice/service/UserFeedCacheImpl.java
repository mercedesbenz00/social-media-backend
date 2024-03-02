package iq.earthlink.social.userfeedaggregatorservice.service;

import io.micrometer.core.annotation.Timed;
import iq.earthlink.social.userfeedaggregatorservice.dto.FeedAggregatorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Log4j2
@RequiredArgsConstructor
public class UserFeedCacheImpl implements UserFeedCache {
    private final RedisTemplate<String, FeedAggregatorResponse> feedCache;
    private final RedisTemplate<String, Long> cacheTimestamp;
    private static final String TIMESTAMP_KEY = "timestamp";

    @Override
    public Long getTimestamp(String mode, Long personId) {
        return cacheTimestamp.opsForValue().get(mode + "_" + personId + "_" + TIMESTAMP_KEY);
    }

    @Override
    public void setTimestamp(String mode, Long personId, long timestamp) {
        cacheTimestamp.opsForValue().set(mode + "_" + personId + "_" + TIMESTAMP_KEY, timestamp);
        cacheTimestamp.expire(mode + "_" + personId + "_" + TIMESTAMP_KEY, 24, TimeUnit.HOURS);
    }

    @Override
    @Timed(value = "userFeed.save.to.cache", description = "Time taken to save user feed to the cache")
    public void saveUserFeed(String key, List<FeedAggregatorResponse> feed) {
        if (!CollectionUtils.isEmpty(feed)) {
            AtomicReference<Double> score = new AtomicReference<>(0.0);
            feed.forEach(feedItem -> {
                feedCache.opsForZSet().add(key, feedItem, score.get());
                score.getAndSet(score.get() + 1);
            });
            feedCache.expire(key, 24, TimeUnit.HOURS);
        }
    }

    @Override
    @Timed(value = "userFeed.invalidate.cache", description = "Time taken to invalidate user feed cache")
    public void invalidateUserFeed(String key) {
        log.info("Invalidating user feed cache for the key: {}", key);
        Set<String> keysToDelete = feedCache.keys(key + "*");
        if (keysToDelete != null) {
            feedCache.delete(keysToDelete);
        }
    }

    @Override
    public void lockFeedCache(String lockKey) {
        log.info("Locking the user feed cache with lockKey: {}", lockKey);
        cacheTimestamp.opsForValue().set(lockKey, 1L);
    }

    @Override
    public void unlockFeedCache(String lockKey) {
        log.info("Unlocking the user feed cache with lockKey: {}", lockKey);
        cacheTimestamp.delete(lockKey);
    }

    @Override
    public Boolean isFeedCacheLocked(String lockKey) {
        log.info("Check the user feed cache for lock with lockKey: {}", lockKey);
        return cacheTimestamp.hasKey(lockKey);
    }

    @Override
    @Timed(value = "userFeed.get.feed.page", description = "Time taken to get user feed page from the cache")
    public Set<FeedAggregatorResponse> getUserFeedInRange(String key, int startIndex, int endIndex) {
        return feedCache.opsForZSet().range(key, startIndex, endIndex);
    }

    @Override
    public boolean isKeyExist(String key) {
        return Boolean.TRUE.equals(feedCache.hasKey(key));
    }

    @Override
    @Timed(value = "userFeed.get.all.feed", description = "Time taken to get whole user feed from the cache")
    public Set<FeedAggregatorResponse> getUserFeed(String key) {
        return feedCache.opsForZSet().range(key, 0, -1);
    }

    @Override
    public long getUserFeedSize(String key) {
        Long total = feedCache.opsForZSet().size(key);
        return total == null ? 0 : total;
    }
}
