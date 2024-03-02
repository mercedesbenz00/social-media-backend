package iq.earthlink.social.postprocessorservice.event;

import io.micrometer.core.annotation.Timed;
import iq.earthlink.social.postprocessorservice.dto.PostEvent;
import iq.earthlink.social.postprocessorservice.dto.PostEventType;
import iq.earthlink.social.postprocessorservice.repository.RecentPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PostEventListener {
    private final RecentPostRepository repository;

    @KafkaListener(topics = "post-event", groupId = "new-post-processor-service",
            containerFactory = "postListenerContainerFactory")
    @Timed("new.post.processor")
    public void newPostListener(@Payload PostEvent post, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) {
        log.info("Received new post event {}", post);
        if (PostEventType.POST_PUBLISHED.equals(post.getEventType())){
            repository.addPostToList(post, key);
        }
    }
}
