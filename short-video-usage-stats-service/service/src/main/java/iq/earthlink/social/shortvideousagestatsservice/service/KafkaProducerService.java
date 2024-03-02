package iq.earthlink.social.shortvideousagestatsservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class KafkaProducerService {
    private static final Logger logger =
            LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Resource(name="kafkaTemplateString")
    private KafkaTemplate<String, String> kafkaStringTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendObjectMessage(String topic, Object message)
    {
        logger.info("Object message sent -> {}", message);
        this.kafkaTemplate.send(topic, message);
    }

    public void sendStringMessage(String topic, String message) {
        logger.info("String message sent -> {}", message);
        this.kafkaStringTemplate.send(topic, message);
    }
}
