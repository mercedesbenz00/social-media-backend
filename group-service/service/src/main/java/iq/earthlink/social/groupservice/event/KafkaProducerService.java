package iq.earthlink.social.groupservice.event;

import iq.earthlink.social.groupservice.event.outbox.MessageStatus;
import iq.earthlink.social.groupservice.event.outbox.model.OutboxMessage;
import iq.earthlink.social.groupservice.event.outbox.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private static final Logger logger =
            LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxMessageRepository outboxMessageRepository;


    @Transactional
    public void sendMessageOnFailureThrowError(String topic, Object message)  {
        try {
            kafkaTemplate.send(topic, message).get();
            logger.info("Message sent to topic {} with payload {}", topic, message);
        } catch (Exception ex) {
            throw new RuntimeException("Could not connect to kafka cluster: " + ex.getMessage());
        }
    }

    @Transactional
    public void sendMessage(String topic, Object message) {
        sendMessage(topic, null, message);
    }

    public void sendMessage(String topic, String key, Object message) {
        try {
            kafkaTemplate.send(topic, key, message).get();
            logger.info("Message sent to topic {} with payload {}", topic, message);
        } catch (Exception ex) {
            logger.error("Error in sending kafka message to topic {} with payload {}, error is: {}", topic, message, ex.getMessage());
            var outboxMessage = OutboxMessage.builder()
                    .topic(topic)
                    .key(key)
                    .payload(message)
                    .status(MessageStatus.PENDING)
                    .createdAt(new Date())
                    .attemptsNumber(1)
                    .build();
            outboxMessageRepository.save(outboxMessage);
        }
    }
}