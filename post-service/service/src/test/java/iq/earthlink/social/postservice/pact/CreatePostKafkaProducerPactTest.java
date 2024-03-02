package iq.earthlink.social.postservice.pact;

import au.com.dius.pact.provider.MessageAndMetadata;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iq.earthlink.social.classes.enumeration.PostType;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.postservice.group.AccessType;
import iq.earthlink.social.postservice.post.model.Post;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Provider("create-post-kafka-producer")
@PactBroker
@IgnoreNoPactsToVerify
@Disabled
class CreatePostKafkaProducerPactTest {
    @Autowired
    private ObjectMapper objectMapper;

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void testTemplate(PactVerificationContext context) {
        if (Objects.nonNull(context))
            context.verifyInteraction();
    }

    @BeforeAll
    static void enablePublishingPact() {
        System.setProperty("pact.verifier.publishResults", "true");
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        if (Objects.nonNull(context))
            context.setTarget(new MessageTestTarget());
    }

    @PactVerifyProvider("Post creation event")
    public MessageAndMetadata postDataEvent() throws JsonProcessingException, ParseException {
        Post post = Post
                .builder()
                .id(1L)
                .postUuid(UUID.fromString("871e70ae-e2ae-c3c9-b11d-c1e15c22d7f1"))
                .authorDisplayName("John Smith")
                .authorId(12L)
                .content("Post content")
                .createdAt(new SimpleDateFormat("yyyy-MM-dd").parse("2023-01-01"))
                .commentsAllowed(false)
                .postType(PostType.ORIGINAL)
                .userGroupId(1L)
                .userGroupType(AccessType.PUBLIC)
                .publishedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2023-01-01"))
                .build();
        Message<String> message = MessageBuilder.withPayload(objectMapper.writeValueAsString(post))
                .setHeader(KafkaHeaders.TOPIC, CommonConstants.POST_EVENT).setHeader("Content-Type", "application/json; charset=utf-8")
                .build();

        return generateMessageAndMetadata(message);

    }

    private MessageAndMetadata generateMessageAndMetadata(Message<String> message) {
        HashMap<String, Object> metadata = new HashMap<>(message.getHeaders());
        return new MessageAndMetadata(message.getPayload().getBytes(), metadata);
    }
}

