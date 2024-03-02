package iq.earthlink.social.postservice.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.databind.ObjectMapper;
import iq.earthlink.social.classes.data.dto.ContentModerationDto;
import iq.earthlink.social.classes.enumeration.ContentType;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.postservice.event.PostEventListener;
import iq.earthlink.social.postservice.post.comment.repository.CommentRepository;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@PactTestFor(providerName = "ai-post-moderator", providerType = ProviderType.ASYNCH)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
class PostModerationKafkaConsumerPactTest {

    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @Mock
    private PostRepository repository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PostEventListener listener;

    @Pact(consumer = "post-service", provider = "ai-post-moderator")
    MessagePact deleteOffensivePost(MessagePactBuilder builder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.JANUARY, 25); //Year, month and day of month
        Date date = cal.getTime();
        dateFormat.format(cal.getTime());

        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("type", ContentType.POST.getDisplayName());
        body.numberType("id", 10);
        body.stringType("aiModel","offensiveLang");
        body.date("analyzedAt","yyyy-MM-dd HH:mm:ss", date);
        body.stringType("reason_key","0000");
        body.stringType("action","delete");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Content-Type", "application/json");
        metadata.put("kafka_topic", CommonConstants.DELETE_TOPIC);

        return builder.expectsToReceive("delete offensive post event").withMetadata(metadata).withContent(body).toPact();
    }

    @Test
    @PactTestFor(pactMethod = "deleteOffensivePost")
    void testRemoveOffensivePost(List<Message> messages) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ContentModerationDto dto = objectMapper.readValue(messages.get(0).contentsAsString(), ContentModerationDto.class);
        assertThat(dto.getId()).isEqualTo(10);
        assertThat(dto.getType()).isEqualTo("post");
        assertThat(dto.getAction()).isEqualTo("delete");
    }

}
