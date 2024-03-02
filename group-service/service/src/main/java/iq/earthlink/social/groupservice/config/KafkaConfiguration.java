package iq.earthlink.social.groupservice.config;

import iq.earthlink.social.classes.data.dto.JsonImageProcessResult;
import iq.earthlink.social.groupservice.person.dto.PersonEventDTO;
import iq.earthlink.social.groupservice.post.dto.PostEventDTO;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
public class KafkaConfiguration {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {

        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        DefaultKafkaProducerFactory<String, Object> defaultKafkaProducerFactory = new DefaultKafkaProducerFactory<>(props);
        defaultKafkaProducerFactory.setTransactionIdPrefix("my.transaction.id.");
        return defaultKafkaProducerFactory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


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
    public ConsumerFactory<String, JsonImageProcessResult> imageServiceConsumerFactory(KafkaProperties kafkaProperties) {
        final JsonDeserializer<JsonImageProcessResult> jsonDeserializer = new JsonDeserializer<>(JsonImageProcessResult.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<JsonImageProcessResult> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), errorHandlingDeserializer);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JsonImageProcessResult> imageServiceListenerContainerFactory(KafkaProperties kafkaProperties) {
        ConcurrentKafkaListenerContainerFactory<String, JsonImageProcessResult> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(imageServiceConsumerFactory(kafkaProperties));
        return factory;
    }

    @Bean
    public ConsumerFactory<String, PersonEventDTO> personServiceConsumerFactory(KafkaProperties kafkaProperties) {
        final JsonDeserializer<PersonEventDTO> jsonDeserializer = new JsonDeserializer<>(PersonEventDTO.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<PersonEventDTO> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), errorHandlingDeserializer);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PersonEventDTO> personServiceListenerContainerFactory(KafkaProperties kafkaProperties) {
        ConcurrentKafkaListenerContainerFactory<String, PersonEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(personServiceConsumerFactory(kafkaProperties));
        return factory;
    }

    @Bean
    public ConsumerFactory<String, PostEventDTO> postEventDTOConsumerFactory() {
        final JsonDeserializer<PostEventDTO> jsonDeserializer = new JsonDeserializer<>(PostEventDTO.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<PostEventDTO> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostEventDTO> postEventListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PostEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(postEventDTOConsumerFactory());
        return factory;
    }

}