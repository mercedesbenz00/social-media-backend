package iq.earthlink.social.postprocessorservice.pact;

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
import iq.earthlink.social.postprocessorservice.dto.PostEvent;
import iq.earthlink.social.postprocessorservice.event.PostEventListener;
import iq.earthlink.social.postprocessorservice.repository.RecentPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "create-post-kafka-producer", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
class CreatePostKafkaConsumerPactTest {

    @Mock
    private RecentPostRepository repository;

    @InjectMocks
    private PostEventListener listener;

    @Pact(consumer = "post-processor-service", provider = "create-post-kafka-producer")
    MessagePact receiveRecentPost(MessagePactBuilder builder) {
        LocalDate date = LocalDate.of(2023, Month.JANUARY, 1);

        PactDslJsonBody body = new PactDslJsonBody();
        body.uuid("postUuid", UUID.fromString("871e70ae-e2ae-c3c9-b11d-c1e15c22d7f1"));
        body.date("publishedAt", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        body.numberType("groupId", 1L);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json; charset=utf-8");
        metadata.put("kafka_topic", "POST_EVENT");

        return builder.expectsToReceive("Post creation event").withMetadata(metadata).withContent(body).toPact();
    }

    @Test
    @PactTestFor(pactMethod = "receiveRecentPost")
    void testReceiveRecentPost(List<Message> messages) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        PostEvent dto = objectMapper.readValue(messages.get(0).contentsAsString(), PostEvent.class);
        LocalDate publishedAt = dto.getPublishedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        assertThat(dto.getPostUuid()).isEqualTo("871e70ae-e2ae-c3c9-b11d-c1e15c22d7f1");
        assertThat(dto.getGroupId()).isEqualTo(1L);
        assertThat(publishedAt.getYear()).isEqualTo(2023);
        assertThat(publishedAt.getMonth()).isEqualTo(Month.JANUARY);
        assertThat(publishedAt.getDayOfMonth()).isEqualTo(1L);
    }

}
