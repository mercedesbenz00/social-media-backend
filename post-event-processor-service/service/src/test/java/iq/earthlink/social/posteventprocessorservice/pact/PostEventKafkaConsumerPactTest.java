package iq.earthlink.social.posteventprocessorservice.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.databind.ObjectMapper;
import iq.earthlink.social.posteventprocessorservice.dto.PostEvent;
import iq.earthlink.social.posteventprocessorservice.event.EventListener;
import iq.earthlink.social.posteventprocessorservice.event.RedisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "post-event-kafka-producer", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
class PostEventKafkaConsumerPactTest {
    @Mock
    private RedisService redisService;

    @InjectMocks
    private EventListener listener;

    @Pact(consumer = "post-event-processor-service", provider = "post-event-kafka-producer")
    MessagePact receivePostViewedEvent(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.uuid("postUuid", UUID.fromString("871e70ae-e2ae-c3c9-b11d-c1e15c22d7f1"));
        body.numberType("userGroupId", 1L);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json; charset=utf-8");
        metadata.put("kafka_topic", "POST_VIEWED");

        return builder.expectsToReceive("Post creation event").withMetadata(metadata).withContent(body).toPact();
    }

    @Pact(consumer = "post-event-processor-service", provider = "post-event-kafka-producer")
    MessagePact receivePostLikedEvent(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.uuid("postUuid", UUID.fromString("871e70ae-e2ae-c3c9-b11d-c1e15c22d7f1"));
        body.numberType("userGroupId", 2L);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json; charset=utf-8");
        metadata.put("kafka_topic", "POST_LIKED");

        return builder.expectsToReceive("Post creation event").withMetadata(metadata).withContent(body).toPact();
    }

    @Pact(consumer = "post-event-processor-service", provider = "post-event-kafka-producer")
    MessagePact receivePostCommentedEvent(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.uuid("postUuid", UUID.fromString("871e70ae-e2ae-c3c9-b11d-c1e15c22d7f1"));
        body.numberType("userGroupId", 3L);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json; charset=utf-8");
        metadata.put("kafka_topic", "POST_COMMENTED");

        return builder.expectsToReceive("Post creation event").withMetadata(metadata).withContent(body).toPact();
    }

    @Test
    @PactTestFor(pactMethod = "receivePostViewedEvent")
    void testReceivePostViewedEvent(List<Message> messages) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        PostEvent dto = objectMapper.readValue(messages.get(0).contentsAsString(), PostEvent.class);
        assertThat(dto.getPostUuid()).isEqualTo("871e70ae-e2ae-c3c9-b11d-c1e15c22d7f1");
        assertThat(dto.getUserGroupId()).isEqualTo(1L);
    }

    @Test
    @PactTestFor(pactMethod = "receivePostLikedEvent")
    void testReceivePostLikedEvent(List<Message> messages) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        PostEvent dto = objectMapper.readValue(messages.get(0).contentsAsString(), PostEvent.class);
        assertThat(dto.getPostUuid()).isEqualTo("871e70ae-e2ae-c3c9-b11d-c1e15c22d7f1");
        assertThat(dto.getUserGroupId()).isEqualTo(2L);
    }

    @Test
    @PactTestFor(pactMethod = "receivePostCommentedEvent")
    void testReceivePostCommentedEvent(List<Message> messages) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        PostEvent dto = objectMapper.readValue(messages.get(0).contentsAsString(), PostEvent.class);
        assertThat(dto.getPostUuid()).isEqualTo("871e70ae-e2ae-c3c9-b11d-c1e15c22d7f1");
        assertThat(dto.getUserGroupId()).isEqualTo(3L);
    }

}
