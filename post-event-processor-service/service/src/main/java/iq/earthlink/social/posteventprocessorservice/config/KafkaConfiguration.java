package iq.earthlink.social.posteventprocessorservice.config;

import iq.earthlink.social.posteventprocessorservice.dto.PostEvent;
import iq.earthlink.social.posteventprocessorservice.event.EventType;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@EnableKafka
@Configuration
public class KafkaConfiguration {

    @Autowired
    private KafkaProperties kafkaProperties;

    private static final String PARTITIONS = "postEventTopics.partitions";
    private static final String REPLICAS = "postEventTopics.replicas";
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
    public NewTopic postViewedTopic() {
        return TopicBuilder.name(EventType.POST_VIEWED.name())
                .partitions(Integer.parseInt(kafkaProperties.getAdmin().getProperties().get(PARTITIONS)))
                .replicas(Integer.parseInt(kafkaProperties.getAdmin().getProperties().get(REPLICAS)))
                .build();
    }

    @Bean
    public NewTopic postLikedTopic() {
        return TopicBuilder.name(EventType.POST_LIKED.name())
                .partitions(Integer.parseInt(kafkaProperties.getAdmin().getProperties().get(PARTITIONS)))
                .replicas(Integer.parseInt(kafkaProperties.getAdmin().getProperties().get(REPLICAS)))
                .build();
    }

    @Bean
    public NewTopic postCommentedTopic() {
        return TopicBuilder.name(EventType.POST_COMMENTED.name())
                .partitions(Integer.parseInt(kafkaProperties.getAdmin().getProperties().get(PARTITIONS)))
                .replicas(Integer.parseInt(kafkaProperties.getAdmin().getProperties().get(REPLICAS)))
                .build();
    }

    @Bean
    public ConsumerFactory<String, PostEvent> postEventConsumerFactory() {
        final JsonDeserializer<PostEvent> jsonDeserializer = new JsonDeserializer<>(PostEvent.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<PostEvent> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), errorHandlingDeserializer);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostEvent> postEventListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PostEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(postEventConsumerFactory());
        return factory;
    }
}