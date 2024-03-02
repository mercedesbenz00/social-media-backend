package iq.earthlink.social.shortvideoregistryservice.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.databind.ObjectMapper;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.shortvideoregistryservice.dto.CreateShortVideoMessageDTO;
import iq.earthlink.social.shortvideoregistryservice.event.ShortVideoEventListener;
import iq.earthlink.social.shortvideoregistryservice.repository.ShortVideoConfigurationRepository;
import iq.earthlink.social.shortvideoregistryservice.repository.ShortVideoRepository;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@PactTestFor(providerName = "short-video-service-kafka-producer", providerType = ProviderType.ASYNCH)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, CassandraAutoConfiguration.class})
@Disabled
class ShortVideoServiceKafkaConsumerPactTest {

    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @Mock
    private ShortVideoRepository repository;

    @Mock
    private ShortVideoConfigurationRepository configurationRepository;

    @InjectMocks
    private ShortVideoEventListener listener;

    @Pact(consumer = "short-video-registry-service", provider = "short-video-service-kafka-producer")
    MessagePact createShortVideoData(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("title", "my file");
        body.numberType("authorId", 1);
        body.stringType("bucket","social-media-short-videos");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");
        metadata.put("kafka_topic", CommonConstants.SHORT_VIDEO_TOPIC);

        return builder.expectsToReceive("a short video data create event").withMetadata(metadata).withContent(body).toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createShortVideoData")
    void testCreateShortVideoData(List<Message> messages) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        CreateShortVideoMessageDTO createShortVideoDTO = objectMapper.readValue(messages.get(0).contentsAsString(), CreateShortVideoMessageDTO.class);
        assertThat(createShortVideoDTO.getAuthorId()).isEqualTo(1L);
        assertThat(createShortVideoDTO.getTitle()).isEqualTo("my file");
        assertThat(createShortVideoDTO.getBucket()).isEqualTo("social-media-short-videos");
    }

}
