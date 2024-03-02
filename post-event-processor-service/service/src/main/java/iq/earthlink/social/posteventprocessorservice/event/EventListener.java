package iq.earthlink.social.posteventprocessorservice.event;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import iq.earthlink.social.posteventprocessorservice.dto.PostEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventListener {

    private final RedisService redisService;
    private final EventTypeProperties eventTypeProperties;
    private final MeterRegistry meterRegistry;

    @Autowired
    public EventListener(RedisService redisService, EventTypeProperties eventTypeProperties, MeterRegistry meterRegistry) {
        this.redisService = redisService;
        this.eventTypeProperties = eventTypeProperties;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics = "POST_VIEWED", groupId = "post-event-processor-service",
            containerFactory = "postEventListenerContainerFactory")
    @Timed("post.event.processor.view")
    public void postViewedListener(@Payload PostEvent post) {
        recordMessageCounter(EventType.POST_VIEWED);
        redisService.sendToRedis(post, eventTypeProperties.getPostViewed());
    }

    @KafkaListener(topics = "POST_LIKED", groupId = "post-event-processor-service",
            containerFactory = "postEventListenerContainerFactory")
    @Timed("post.event.processor.like")
    public void postLikedListener(@Payload PostEvent post) {
        recordMessageCounter(EventType.POST_LIKED);
        redisService.sendToRedis(post, eventTypeProperties.getPostLiked());
    }

    @KafkaListener(topics = "POST_COMMENTED", groupId = "post-event-processor-service",
            containerFactory = "postEventListenerContainerFactory")
    @Timed("post.event.processor.comment")
    public void postCommentedListener(@Payload PostEvent post) {
        recordMessageCounter(EventType.POST_COMMENTED);
        redisService.sendToRedis(post, eventTypeProperties.getPostCommented());
    }

    private void recordMessageCounter(EventType eventType) {
        Counter counter = Counter.builder("post.event.processed.count")
                .tags("sourceTopic", eventType.name())
                .register(meterRegistry);
        counter.increment();
    }
}
