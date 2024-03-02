package iq.earthlink.social.shortvideoregistryservice.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import iq.earthlink.social.common.filestorage.FileStorage;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.common.util.FileUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.shortvideoregistryservice.dto.CreateShortVideoMessageDTO;
import iq.earthlink.social.shortvideoregistryservice.model.Category;
import iq.earthlink.social.shortvideoregistryservice.model.ShortVideo;
import iq.earthlink.social.shortvideoregistryservice.model.ShortVideoConfiguration;
import iq.earthlink.social.shortvideoregistryservice.repository.CategoryRepository;
import iq.earthlink.social.shortvideoregistryservice.repository.ShortVideoConfigurationRepository;
import iq.earthlink.social.shortvideoregistryservice.repository.ShortVideoRepository;
import iq.earthlink.social.shortvideoregistryservice.service.ShortVideoRegistryService;
import iq.earthlink.social.shortvideoregistryservice.util.CustomMultiPartFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;

@Profile("kafka")
@Component
public class ShortVideoEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortVideoEventListener.class);

    public static final String THUMBNAIL_SUFFIX = "_thumbnail";
    public static final String THUMBNAIL_FORMAT = "png";
    private final CassandraTemplate cassandraTemplate;
    private final ShortVideoRepository repository;

    private final ShortVideoConfigurationRepository shortVideoConfigurationRepository;
    private final ShortVideoRegistryService shortVideoRegistryService;
    private final CategoryRepository categoryRepository;
    private final FileStorageProvider fileStorageProvider;
    private final Mapper mapper;
    private final ObjectMapper objectMapper;

    public ShortVideoEventListener(CassandraTemplate cassandraTemplate,
                                   ShortVideoRepository repository,
                                   ShortVideoConfigurationRepository shortVideoConfigurationRepository,
                                   ShortVideoRegistryService shortVideoRegistryService,
                                   CategoryRepository categoryRepository,
                                   FileStorageProvider fileStorageProvider, Mapper mapper,
                                   ObjectMapper objectMapper) {
        this.cassandraTemplate = cassandraTemplate;
        this.repository = repository;
        this.shortVideoConfigurationRepository = shortVideoConfigurationRepository;
        this.shortVideoRegistryService = shortVideoRegistryService;
        this.categoryRepository = categoryRepository;
        this.fileStorageProvider = fileStorageProvider;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = CommonConstants.SHORT_VIDEO_TOPIC, groupId = "group-id",
            containerFactory = "kafkaMetaDataListenerContainerFactory")
    public ShortVideo shortVideoDataListener(@Payload CreateShortVideoMessageDTO cr) {
        LOGGER.info("Received an event to save the short video");
        ShortVideo saved;
        InputStream is = null;
        try {
            Optional<ShortVideo> sv = repository.findById(cr.getId());
            if (sv.isPresent()) {
                return null;
            }
            saved = saveShortVideoData(cr);
            if (StringUtils.isNotEmpty(cr.getUrl())) {
                // External upload:
                FileStorage storage = fileStorageProvider.getStorage(StorageType.MINIO);
                String fileName = cr.getId().toString();
                URL url = new URL(cr.getUrl());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                is = urlConnection.getInputStream();
                urlConnection.setReadTimeout((1000 * 30)); //30 min
                storage.upload(is, urlConnection.getContentLength(), urlConnection.getContentType(), fileName, fileName);
            }
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return saved;
    }

    @KafkaListener(topics = CommonConstants.SHORT_VIDEO_NOTIFICATION_TOPIC, groupId = "group-id",
            containerFactory = "kafkaMinioListenerContainerFactory")
    public ShortVideo minioNotificationListener(ConsumerRecord<String, Object> consumerRecord) {

        String shortVideoId = StringUtils.substringBefore(StringUtils.substringAfter(consumerRecord.key(), "/"), "_");
        Optional<ShortVideo> shortVideo = repository.findById(UUID.fromString(shortVideoId));

        if (shortVideo.isPresent()) {
            ShortVideo sv = shortVideo.get();
            createThumbnail(sv.getId().toString());
            Map<String, String> metadata = sv.getMetadata() != null ? sv.getMetadata() : new HashMap<>();
            if (MapUtils.isEmpty(metadata) || !metadata.containsKey("minio")) {
                try {
                    CassandraBatchOperations batchOps = cassandraTemplate.batchOps();
                    metadata.put("minio", consumerRecord.value().toString());

                    sv.setMetadata(objectMapper.convertValue(metadata, new TypeReference<>() {
                    }));

                    shortVideoRegistryService.saveShortVideoOfCategory(sv, batchOps);

                    // Check if video was not uploaded by third party:
                    if (sv.getAuthorId() > 0) {
                        // User uploaded video using presigned url:
                        shortVideoRegistryService.saveShortVideoOfAuthor(sv, batchOps);
                        shortVideoRegistryService.saveShortVideoOfFriends(Collections.emptySet(), sv, batchOps);
                    }

                    batchOps.insert(sv);
                    batchOps.execute();
                    return sv;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @KafkaListener(topics = CommonConstants.FRIEND_ADDED_TOPIC, groupId = "group-id",
            containerFactory = "kafkaFriendsListenerContainerFactory")
    public ShortVideo addFriendListener(@Payload JsonPerson person) {
        LOGGER.info("Received FRIEND_ADDED event. Added person with ID: {}", person.getId());
        return null;
    }

    @KafkaListener(topics = CommonConstants.FRIEND_REMOVED_TOPIC, groupId = "group-id",
            containerFactory = "kafkaFriendsListenerContainerFactory")
    public ShortVideo removeFriendListener(@Payload JsonPerson person) {
        LOGGER.info("Received FRIEND_REMOVED event. Removed person with ID: {}", person.getId());
        return null;
    }

    private ShortVideo saveShortVideoData(CreateShortVideoMessageDTO cr) {
        CassandraBatchOperations batchOps = cassandraTemplate.batchOps();

        try {
            ShortVideo shortVideo = getShortVideoEntity(cr, batchOps);

            // Add configuration settings:
            ShortVideoConfiguration shortVideoConfiguration = shortVideoConfigurationRepository
                    .findByPersonId(shortVideo.getAuthorId())
                    .orElseGet(() -> {
                        ShortVideoConfiguration scf = ShortVideoConfiguration
                                .builder()
                                .id(UUID.randomUUID())
                                .privacyLevel(PrivacyLevel.PUBLIC)
                                .personId(shortVideo.getAuthorId())
                                .selectedUsers(new HashSet<>())
                                .selectedGroups(new HashSet<>())
                                .commentsAllowed(false)
                                .build();
                        batchOps.insert(scf);
                        return scf;
                    });

            shortVideo.setPrivacyLevel(shortVideoConfiguration.getPrivacyLevel());
            shortVideo.setSelectedUsers(shortVideoConfiguration.getSelectedUsers());
            shortVideo.setSelectedGroups(shortVideoConfiguration.getSelectedGroups());
            shortVideo.setCommentsAllowed(shortVideoConfiguration.getCommentsAllowed());

            if (cr.getAuthorId() != null) {
                shortVideoRegistryService.saveShortVideoOfAuthor(shortVideo, batchOps);
                shortVideoRegistryService.saveShortVideoOfFriends(cr.getFriends(), shortVideo, batchOps);
                shortVideoRegistryService.saveAuthorFriends(cr.getFriends(), shortVideo.getAuthorId(), batchOps);
            } else {
                // Video was uploaded by third party:
                Map<String, String> externalData = new HashMap<>();
                Map<String, String> metadata = new HashMap<>();

                Map<String, Long> videoStats = new HashMap<>();
                videoStats.put("videoDuration", cr.getVideoDuration());
                videoStats.put("likesCount", cr.getLikesCount());
                videoStats.put("commentsCount", cr.getCommentsCount());
                videoStats.put("playCount", cr.getPlayCount());

                externalData.put("videoStats", videoStats.toString());
                metadata.put("external", externalData.toString());
                shortVideo.setMetadata(objectMapper.convertValue(metadata, new TypeReference<>() {
                }));
            }

            shortVideoRegistryService.saveShortVideoOfCategory(shortVideo, batchOps);
            batchOps.insert(shortVideo);
            batchOps.execute();

            return shortVideo;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private ShortVideo getShortVideoEntity(CreateShortVideoMessageDTO cr, CassandraBatchOperations batchOps) {
        ShortVideo shortVideo = mapper.map(cr, ShortVideo.class);

        if (!CollectionUtils.isEmpty(cr.getCategories())) {
            cr.getCategories().forEach(category -> {
                Category shortVideoCategory = categoryRepository
                        .findByName(category.getName())
                        .orElseGet(() -> {
                            Category cat = Category
                                    .builder()
                                    .id(category.getCategoryId())
                                    .name(category.getName())
                                    .build();
                            batchOps.insert(cat);
                            return cat;
                        });
                shortVideo.getCategories().add(shortVideoCategory.getId());
            });
        }

        shortVideo.setId(UUID.randomUUID());
        shortVideo.setYear(DateUtil.getYear(new Date()));
        shortVideo.setId(UUID.fromString(cr.getId().toString()));
        shortVideo.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        shortVideo.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return shortVideo;
    }

    private void createThumbnail(String fileName) {
        FileStorage storage = fileStorageProvider.getStorage(StorageType.MINIO);
        String thumbnailFileName = fileName + THUMBNAIL_SUFFIX;
        String thumbnailUrl = storage.getUrl(thumbnailFileName);

        if (Objects.isNull(thumbnailUrl)) {
            // Generate thumbnail:
            try {
                // Get video file from minio:
                InputStream vis = storage.download(fileName);

                FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(vis);
                frameGrabber.start();

                BufferedImage bi;
                try (Java2DFrameConverter frameConverter = new Java2DFrameConverter()) {
                    Frame f = frameGrabber.grabKeyFrame();
                    bi = frameConverter.convert(f);
                }

                frameGrabber.stop();

                if (bi != null) {
                    MultipartFile multipartFile = new CustomMultiPartFile(thumbnailFileName, null,
                            FileUtil.IMAGE_PREFIX + THUMBNAIL_FORMAT, FileUtil.toByteArray(bi, THUMBNAIL_FORMAT));
                    storage.upload(multipartFile, thumbnailFileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
