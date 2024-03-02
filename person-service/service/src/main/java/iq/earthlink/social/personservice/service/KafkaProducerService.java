package iq.earthlink.social.personservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KafkaProducerService {
    private static final Logger logger =
            LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void sendMessageOnFailureThrowError(String topic, Object message) {
        try {
            kafkaTemplate.send(topic, message).get();
            logger.info("Message sent to topic {} with payload {}", topic, message);
        } catch (Exception ex) {
            throw new RuntimeException("Could not connect to kafka cluster: " + ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void sendMessage(String topic, Object message) {
        logger.info("Message sent -> {}", message);
        this.kafkaTemplate.send(topic, message);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void sendMessageWithKey(String topic, String key, Object message) {
        logger.info("Message sent -> {}", message);
        this.kafkaTemplate.send(topic, key, message);
    }
}
