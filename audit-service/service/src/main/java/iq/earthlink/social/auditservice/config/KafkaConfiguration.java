package iq.earthlink.social.auditservice.config;

import iq.earthlink.social.auditservice.dto.JsonAuditLog;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConfiguration {

    @Autowired
    private KafkaProperties kafkaProperties;

    /**
     * Boot will autowire this into the container factory.
     */
    @Bean
    public CommonErrorHandler errorHandler(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
        return new DefaultErrorHandler(deadLetterPublishingRecoverer);
    }

    /**
     * Configure the {@link DeadLetterPublishingRecoverer} to publish poison pill bytes to a dead letter topic:
     */
    @Bean
    public DeadLetterPublishingRecoverer publisher(KafkaTemplate<?, ?> bytesTemplate) {
        return new DeadLetterPublishingRecoverer(bytesTemplate);
    }

    @Bean
    public ConsumerFactory<String, JsonAuditLog> auditLogConsumerFactory() {
        final JsonDeserializer<JsonAuditLog> jsonDeserializer = new JsonDeserializer<>(JsonAuditLog.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<JsonAuditLog> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JsonAuditLog> auditLogListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, JsonAuditLog> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(auditLogConsumerFactory());
        return factory;
    }

}
