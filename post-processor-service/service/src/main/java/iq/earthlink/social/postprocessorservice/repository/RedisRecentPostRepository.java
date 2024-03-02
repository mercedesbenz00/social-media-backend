package iq.earthlink.social.postprocessorservice.repository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import iq.earthlink.social.postprocessorservice.dto.PostEvent;
import iq.earthlink.social.postprocessorservice.dto.RecentPost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRecentPostRepository implements RecentPostRepository {

    private final RedisTemplate<String, RecentPost> redisTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${social.maxPostsPerGroup}")
    private int maxPostsPerGroup;

    public RedisRecentPostRepository(RedisTemplate<String, RecentPost> redisTemplate, CompositeMeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void addPostToList(PostEvent post, String messageKey) {
        String key = "group:" + post.getGroupId() + ":posts";
        RecentPost recentPost = RecentPost
                .builder()
                .postUuid(post.getPostUuid())
                .publishedAt(post.getPublishedAt())
                .build();
        redisTemplate.opsForList().leftPush(key, recentPost);
        redisTemplate.opsForList().trim(key, 0, maxPostsPerGroup - 1L);
        Counter counter = Counter.builder("post.processed.count").tags("partition", messageKey).register(meterRegistry);
        counter.increment();
    }
}
