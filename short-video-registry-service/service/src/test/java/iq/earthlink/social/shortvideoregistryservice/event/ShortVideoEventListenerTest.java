package iq.earthlink.social.shortvideoregistryservice.event;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioFileStorage;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.shortvideoregistryservice.dto.CreateShortVideoMessageDTO;
import iq.earthlink.social.shortvideoregistryservice.model.ShortVideo;
import iq.earthlink.social.shortvideoregistryservice.model.ShortVideoConfiguration;
import iq.earthlink.social.shortvideoregistryservice.repository.ShortVideoConfigurationRepository;
import iq.earthlink.social.shortvideoregistryservice.repository.ShortVideoRepository;
import iq.earthlink.social.shortvideoregistryservice.repository.ShortVideosByAuthorRepository;
import iq.earthlink.social.shortvideoregistryservice.service.ShortVideoRegistryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, CassandraAutoConfiguration.class})
@Disabled
class ShortVideoEventListenerTest {

    @Mock
    private ShortVideoRepository repository;

    @Mock
    private ShortVideosByAuthorRepository shortVideoByAuthorRepository;

    @Mock
    private CassandraTemplate cassandraTemplate;

    @Mock
    private ShortVideoRegistryService shortVideoRegistryService;

    @Mock
    private CassandraBatchOperations cassandraBatchOperations;

    @Mock
    ShortVideoConfigurationRepository configurationRepository;

    @Mock
    FileStorageProvider fileStorageProvider;

    @Mock
    MinioFileStorage fileStorage;

    @Spy
    Mapper mapper = DozerBeanMapperBuilder.create().build();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @InjectMocks
    private ShortVideoEventListener eventListener;


    @Test
    void whenSaveShortVideoMetaData_ReturnSavedObject() {
        //given
        CreateShortVideoMessageDTO createShortVideoDto = new CreateShortVideoMessageDTO();
        createShortVideoDto.setId(UUID.fromString("4c7746ea-3f1f-4133-8f74-51449c2a97ba"));
        createShortVideoDto.setAuthorId(1L);
        createShortVideoDto.setBucket("social-media-short-videos");
        createShortVideoDto.setTitle("Title");

        ShortVideo savedShortVideo = new ShortVideo();
        savedShortVideo.setId(UUID.randomUUID());
        savedShortVideo.setPrivacyLevel(PrivacyLevel.PUBLIC);
        savedShortVideo.setAuthorId(1L);
        when(cassandraTemplate.batchOps()).thenReturn(cassandraBatchOperations);
        when(cassandraBatchOperations.insert(any(Object.class))).thenReturn(cassandraBatchOperations);
        when(configurationRepository.findByPersonId(any(Long.class))).thenReturn(Optional.ofNullable(ShortVideoConfiguration
                .builder()
                .privacyLevel(PrivacyLevel.PUBLIC)
                .personId(1)
                .selectedUsers(Set.of())
                .selectedGroups(Set.of())
                .build()));
        willDoNothing().given(shortVideoRegistryService).saveShortVideoOfAuthor(savedShortVideo, cassandraBatchOperations);
        willDoNothing().given(shortVideoRegistryService).saveShortVideoOfCategory(savedShortVideo, cassandraBatchOperations);
        willDoNothing().given(shortVideoRegistryService).saveShortVideoOfFriends(Set.of(), savedShortVideo, cassandraBatchOperations);
        willDoNothing().given(shortVideoRegistryService).saveAuthorFriends(Set.of(), savedShortVideo.getAuthorId(), cassandraBatchOperations);

        //when
        ShortVideo shortVideo = eventListener.shortVideoDataListener(createShortVideoDto);

        //then
        Assert.isTrue(shortVideo != null, "Short video object cannot be null");
        assertThat(shortVideo.getBucket()).isEqualTo(createShortVideoDto.getBucket());
        assertThat(shortVideo.getAuthorId()).isEqualTo(createShortVideoDto.getAuthorId());
    }

    @Test
    void whenSaveMinioNotification_ReturnUpdatedObject() {
        //given
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Key", "social-media-short-videos/948496b9-332b-4ed6-8c83-fbdda5b5c8a9");
        String thumbnailUrl = "http://localhost:9000/social-media-short-videos/948496b9-332b-4ed6-8c83-fbdda5b5c8a9_thumbnail";

        ConsumerRecord<String, Object> consumerRecord = new ConsumerRecord<>(
                CommonConstants.SHORT_VIDEO_NOTIFICATION_TOPIC,
                0,
                0,
                "social-media-short-videos/948496b9-332b-4ed6-8c83-fbdda5b5c8a9",
                metadata);

        ShortVideo sv = new ShortVideo();
        sv.setAuthorId(1L);
        sv.setPrivacyLevel(PrivacyLevel.PUBLIC);
        sv.setTitle("Title");
        Optional<ShortVideo> savedShortVideo = Optional.of(sv);

        when(cassandraTemplate.batchOps()).thenReturn(cassandraBatchOperations);
        when(cassandraBatchOperations.insert(any(Object.class))).thenReturn(cassandraBatchOperations);
        when(repository.findById(UUID.fromString("948496b9-332b-4ed6-8c83-fbdda5b5c8a9"))).thenReturn(savedShortVideo);
        savedShortVideo.get().setMetadata(metadata);
        when(repository.save(any(ShortVideo.class))).thenReturn(savedShortVideo.get());
        when(fileStorageProvider.getStorage(StorageType.MINIO)).thenReturn(fileStorage);
        when(fileStorage.getUrl(any(String.class))).thenReturn(thumbnailUrl);

        //when
        ShortVideo shortVideo = eventListener.minioNotificationListener(consumerRecord);

        //then
        assertEquals(shortVideo.getMetadata().get("minio"), metadata.toString());
    }
}