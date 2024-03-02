package iq.earthlink.social.shortvideoservice.pact;

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
import iq.earthlink.social.common.filestorage.FileStorage;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.shortvideoregistryservice.dto.CreateShortVideoMessageDTO;
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

import java.util.HashMap;
import java.util.Objects;

@Provider("short-video-service-kafka-producer")
@IgnoreNoPactsToVerify
@PactBroker
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Disabled
class ShortVideoServiceKafkaProducerPactTest {
    @Autowired
    private FileStorageProvider fileStorageProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void testTemplate(PactVerificationContext context) {
        if (Objects.nonNull(context))
            context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        if (Objects.nonNull(context))
            context.setTarget(new MessageTestTarget());
    }

    @PactVerifyProvider("a short video data create event")
    public MessageAndMetadata addShortVideoDataEvent() throws JsonProcessingException {
        FileStorage storage = fileStorageProvider.getStorage(StorageType.MINIO);
        CreateShortVideoMessageDTO shortVideoDTO = CreateShortVideoMessageDTO
                .builder()
                .authorId(1L)
                .bucket(storage.getBucketName())
                .title("my file")
                .build();
        Message<String> message = MessageBuilder.withPayload(objectMapper.writeValueAsString(shortVideoDTO))
                .setHeader(KafkaHeaders.TOPIC, CommonConstants.SHORT_VIDEO_TOPIC).setHeader("Content-Type", "application/json; charset=utf-8")
                .build();

        return generateMessageAndMetadata(message);

    }

    private MessageAndMetadata generateMessageAndMetadata(Message<String> message) {
        HashMap<String, Object> metadata = new HashMap<String, Object>(message.getHeaders());

        return new MessageAndMetadata(message.getPayload().getBytes(), metadata);
    }
}
