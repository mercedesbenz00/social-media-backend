package iq.earthlink.social.postservice.config;

import iq.earthlink.social.classes.data.dto.ContentModerationDto;
import iq.earthlink.social.classes.data.dto.JsonImageProcessResult;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.postservice.group.dto.GroupEventDTO;
import iq.earthlink.social.postservice.group.member.dto.GroupMemberEventDTO;
import iq.earthlink.social.postservice.group.notificationsettings.dto.UserGroupNotificationSettingsEventDTO;
import iq.earthlink.social.postservice.person.dto.PersonEventDTO;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public NewTopic postEventTopic() {
        return TopicBuilder.name(CommonConstants.POST_EVENT)
                .partitions(Integer.parseInt(kafkaProperties.getAdmin().getProperties().get("postEventTopic.partitions")))
                .replicas(Integer.parseInt(kafkaProperties.getAdmin().getProperties().get("postEventTopic.replicas")))
                .build();
    }

    @Bean
    public ConsumerFactory<String, ContentModerationDto> consumerFactory() {
        final JsonDeserializer<ContentModerationDto> jsonDeserializer = new JsonDeserializer<>(ContentModerationDto.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<ContentModerationDto> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(),
                errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ContentModerationDto> kafkaMetaDataListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ContentModerationDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, JsonImageProcessResult> imageServiceConsumerFactory() {
        final JsonDeserializer<JsonImageProcessResult> jsonDeserializer = new JsonDeserializer<>(JsonImageProcessResult.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<JsonImageProcessResult> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), errorHandlingDeserializer);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JsonImageProcessResult> imageServiceListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, JsonImageProcessResult> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(imageServiceConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, PersonEventDTO> personServiceConsumerFactory() {
        final JsonDeserializer<PersonEventDTO> jsonDeserializer = new JsonDeserializer<>(PersonEventDTO.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<PersonEventDTO> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), errorHandlingDeserializer);
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PersonEventDTO> personServiceListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PersonEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(personServiceConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, GroupEventDTO> groupServiceConsumerFactory() {
        final JsonDeserializer<GroupEventDTO> jsonDeserializer = new JsonDeserializer<>(GroupEventDTO.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<GroupEventDTO> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, GroupEventDTO> groupServiceListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, GroupEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(groupServiceConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, GroupMemberEventDTO> groupMemberServiceConsumerFactory() {
        final JsonDeserializer<GroupMemberEventDTO> jsonDeserializer = new JsonDeserializer<>(GroupMemberEventDTO.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<GroupMemberEventDTO> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, GroupMemberEventDTO> groupMemberServiceListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, GroupMemberEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(groupMemberServiceConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, UserGroupNotificationSettingsEventDTO> notificationSettingsServiceConsumerFactory() {
        final JsonDeserializer<UserGroupNotificationSettingsEventDTO> jsonDeserializer = new JsonDeserializer<>(UserGroupNotificationSettingsEventDTO.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.ignoreTypeHeaders();

        ErrorHandlingDeserializer<UserGroupNotificationSettingsEventDTO> errorHandlingDeserializer
                = new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserGroupNotificationSettingsEventDTO> notificationSettingsServiceListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserGroupNotificationSettingsEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationSettingsServiceConsumerFactory());
        return factory;
    }
}