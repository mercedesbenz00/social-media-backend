package iq.earthlink.social.postservice.story;

import feign.FeignException;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonSizedImage;
import iq.earthlink.social.classes.data.dto.JsonStoryConfiguration;
import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.data.event.NotificationEvent;
import iq.earthlink.social.classes.enumeration.ContentType;
import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.classes.enumeration.StoryAccessType;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.file.MediaFileRepository;
import iq.earthlink.social.common.file.MediaFileTranscodedRepository;
import iq.earthlink.social.common.file.SizedImageRepository;
import iq.earthlink.social.common.file.rest.AbstractMediaService;
import iq.earthlink.social.common.file.rest.DownloadUtils;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.minio.MinioProperties;
import iq.earthlink.social.common.rest.RestPageImpl;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.common.util.FileUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.personservice.person.rest.JsonFollowerNotificationSettings;
import iq.earthlink.social.personservice.person.rest.JsonFollowing;
import iq.earthlink.social.personservice.rest.FollowingRestService;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.postservice.event.KafkaProducerService;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.story.model.Story;
import iq.earthlink.social.postservice.story.model.StoryConfiguration;
import iq.earthlink.social.postservice.story.rest.JsonStory;
import iq.earthlink.social.util.ExceptionUtil;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.dozer.Mapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static iq.earthlink.social.classes.enumeration.StoryAccessType.SELECTED_FOLLOWERS;
import static iq.earthlink.social.common.util.CommonConstants.POST_SERVICE;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;
import static java.util.stream.Collectors.groupingBy;

@Service
public class DefaultStoryManager extends AbstractMediaService implements StoryManager {

    private static final String ERROR_CHECK_NOT_NULL = "error.check.not.null";
    private static final String PERSON = "person";
    private static final String PERSON_ID = "personId";
    private final StoryRepository repository;
    private final FollowingRestService followingRestService;
    private final PersonRestService personRestService;
    private final StoryConfigurationRepository configurationRepository;
    private final KafkaProducerService kafkaProducerService;
    private final PostStoryProperties properties;
    private final Mapper mapper;

    public DefaultStoryManager(StoryRepository repository,
                               MediaFileRepository fileRepository,
                               FileStorageProvider storageProvider,
                               FollowingRestService followingRestService,
                               PersonRestService personRestService,
                               StoryConfigurationRepository configurationRepository,
                               PostStoryProperties properties,
                               MediaFileTranscodedRepository mediaFileTranscodedRepository,
                               SizedImageRepository sizedImageRepository,
                               MinioProperties minioProperties,
                               KafkaProducerService kafkaProducerService, Mapper mapper) {
        super(fileRepository,
                mediaFileTranscodedRepository,
                sizedImageRepository,
                storageProvider,
                minioProperties);

        this.repository = repository;
        this.followingRestService = followingRestService;
        this.personRestService = personRestService;
        this.configurationRepository = configurationRepository;
        this.properties = properties;
        this.kafkaProducerService = kafkaProducerService;
        this.mapper = mapper;
    }

    @Transactional
    @Nonnull
    @Override
    public JsonStory createStory(String authorizationHeader,
                                 @Nonnull PersonDTO person,
                                 @Nonnull MultipartFile file,
                                 @Nullable List<String> references) {
        checkNotNull(person, ERROR_CHECK_NOT_NULL, PERSON);
        checkNotNull(file, ERROR_CHECK_NOT_NULL, "file");

        var storyConfiguration = getStoryConfiguration(person.getPersonId());

        Story story = Story.builder()
                .authorId(person.getPersonId())
                .personReferences(getReferences(references))
                .accessType(storyConfiguration.getAccessType())
                .selectedFollowerIds(new HashSet<>(storyConfiguration.getAllowedFollowersIds()))
                .build();
        story = repository.save(story);
        JsonStory jsonStory = mapper.map(story, JsonStory.class);

        // Send event to notify followers:
        NotificationEvent event = NotificationEvent
                .builder()
                .eventAuthor(PersonData
                        .builder()
                        .id(person.getPersonId())
                        .displayName(person.getDisplayName())
                        .avatar(person.getAvatar())
                        .build())
                .receiverIds(getNotMutedFollowers(authorizationHeader, person.getPersonId(), jsonStory))
                .type(NotificationType.STORY_CREATED)
                .metadata(Map.of(
                        "routeTo", ContentType.STORY.name(),
                        "storyId", jsonStory.getId().toString())).build();

        kafkaProducerService.sendMessage("PUSH_NOTIFICATION", event);


        uploadMediaFile(jsonStory.getId(), file);

        return enrichStoryWithMedia(jsonStory);
    }

    @Nonnull
    @Override
    public JsonStory getStory(Long id) {
        checkNotNull(id, ERROR_CHECK_NOT_NULL, "id");

        Story story = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.not.found.story", id));

        JsonStory jsonStory = mapper.map(story, JsonStory.class);
        return enrichStoryWithMedia(jsonStory);
    }

    @Nonnull
    @Override
    public Page<JsonStory> findStories(String authorizationHeader, Long personId, List<Long> ownerIds, boolean unseenOnly, Pageable page) {
        checkNotNull(page, ERROR_CHECK_NOT_NULL, "page");

        Set<Long> authorIds = new HashSet<>();
        if (CollectionUtils.isNotEmpty(ownerIds)) {
            authorIds.addAll(ownerIds);
        }

        // Find person's subscriptions:
        RestPageImpl<JsonFollowing> followings = followingRestService.findSubscriptions(authorizationHeader,
                personId, 0, Integer.MAX_VALUE);
        List<Long> subscriptionIds = followings.isEmpty() ? new ArrayList<>()
                : followings.stream().map(f -> f.getSubscribedTo().getId()).toList();

        // Find the latest story created date based on configuration setting:
        Date createdAt = DateUtil.getDateBefore(Integer.parseInt(properties.getStoryLifetimeDays()));

        // Find allowed stories sorted by access type, the newest stories first (older than story lifetime):
        Page<Story> stories = unseenOnly ? repository.findAllowedUnseenStoriesAndSort(personId, authorIds, subscriptionIds, createdAt, page)
                : repository.findAllowedStoriesAndSortUnseenFirst(personId, authorIds, subscriptionIds, createdAt, page);

        return stories.map(story -> {
            JsonStory jsonStory = mapper.map(story, JsonStory.class);
            return enrichStoryWithMedia(jsonStory);
        });
    }

    @Transactional
    @Override
    public void removeStory(Long personId, Long id) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkNotNull(id, ERROR_CHECK_NOT_NULL, "id");

        Story story = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.not.found.story", id));

        if (Objects.equals(personId, story.getAuthorId())) {
            try {
                removeFiles(id, MediaFileType.STORY);

                repository.delete(story);
                LOGGER.info("Removed story: {} by person: {}", story, personId);
            } catch (Exception ex) {
                throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error.unable.delete.story", id);
            }
        } else {
            throw new ForbiddenException("error.operation.not.permitted");
        }
    }

    @Nonnull
    @Override
    public ResponseEntity<Resource> downloadMedia(String rangeHeader, Long storyId) {
        checkNotNull(storyId, ERROR_CHECK_NOT_NULL, "story");
        MediaFile file = null;
        List<MediaFile> mediaFiles = findFiles(storyId, MediaFileType.STORY);

        if (!mediaFiles.isEmpty()) {
            file = mediaFiles.get(0);
        }
        return DownloadUtils.fileResponse(file,
                downloadFile(file, DownloadUtils.getOffset(rangeHeader), DownloadUtils.getLength(file, rangeHeader)), rangeHeader);
    }

    @Override
    public void setStoryConfiguration(Long personId, JsonStoryConfiguration configuration) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        checkConfiguration(configuration);

        StoryConfiguration storyConfiguration = configurationRepository.findByPersonId(personId)
                .orElse(StoryConfiguration
                        .builder()
                        .personId(personId)
                        .allowedFollowersIds(Collections.emptySet())
                        .build());

        storyConfiguration.getAllowedFollowersIds().clear();

        storyConfiguration.setAccessType(configuration.getAccessType());
        if (SELECTED_FOLLOWERS.equals(configuration.getAccessType())) {
            storyConfiguration.setAllowedFollowersIds(configuration.getAllowedFollowersIds());
        }
        configurationRepository.save(storyConfiguration);
    }

    @NotNull
    @Override
    public StoryConfiguration getStoryConfiguration(Long personId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, PERSON_ID);
        StoryConfiguration storyConfiguration;
        Optional<StoryConfiguration> configuration = configurationRepository.findByPersonId(personId);
        if (configuration.isPresent())
            storyConfiguration = configuration.get();
        else {
            storyConfiguration = StoryConfiguration
                    .builder()
                    .personId(personId)
                    .accessType(StoryAccessType.ALL_FOLLOWERS)
                    .allowedFollowersIds(new HashSet<>())
                    .build();
            configurationRepository.save(storyConfiguration);
        }
        return storyConfiguration;
    }

    @Nonnull
    @Override
    public JsonStory enrichStoryWithMedia(JsonStory story) {
        List<MediaFile> mediaFiles = findFiles(story.getId(), MediaFileType.STORY);
        if (!mediaFiles.isEmpty()) {
            MediaFile mediaFile = mediaFiles.get(0);
            JsonMediaFile file = mapper.map(mediaFile, JsonMediaFile.class);
            String fileUrl = getFileUrl(mediaFile);
            file.setPath(fileUrl);
            if (CollectionUtils.isNotEmpty(mediaFile.getSizedImages())) {
                Map<String, List<JsonSizedImage>> sizedImages = mediaFile.getSizedImages().stream()
                        .map(sizedImage -> JsonSizedImage
                                .builder()
                                .imageSizeType(sizedImage.getImageSizeType())
                                .size(sizedImage.getSize())
                                .path(getFileUrl(mediaFile.getStorageType(), sizedImage.getPath()))
                                .createdAt(sizedImage.getCreatedAt())
                                .mimeType(sizedImage.getMimeType())
                                .build()
                        ).collect(groupingBy(image -> image.getMimeType().replace("image/", "")));
                file.setSizedImages(sizedImages);
            } else {
                file.setSizedImages(null);
            }
            story.setFile(file);
        }
        return story;
    }

    private void checkConfiguration(JsonStoryConfiguration configuration) {
        if (SELECTED_FOLLOWERS.equals(configuration.getAccessType()) && CollectionUtils.isEmpty(configuration.getAllowedFollowersIds()))
            throw new BadRequestException("error.empty.or.wrong.followers.list");
    }

    private void uploadMediaFile(@NonNull Long storyId, @NonNull MultipartFile file) {

        if (FileUtil.isImage(file) || FileUtil.isVideo(file)) {
            try {
                FileUtil.validateImageOrVideoFiles(Collections.singletonList(file), Long.parseLong(properties.getImageMaxSize()),
                        Long.parseLong(properties.getVideoMaxSize()));

                MediaFile mediaFile = uploadFile(storyId, MediaFileType.STORY, file);

                if (mediaFile.getMimeType().startsWith("image")) {
                    kafkaProducerService.sendMessage(CommonConstants.MEDIA_TO_PROCESS, getImageProcessRequest(mediaFile, POST_SERVICE));
                }
                LOGGER.debug("Uploaded file story file: {} with path: {}", file, mediaFile.getPath());
            } catch (MaxUploadSizeExceededException ex) {
                throw new BadRequestException("error.file.upload.size.exceeded", ex.getMaxUploadSize());
            } catch (Exception ex) {
                throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error.failed.upload.file", file.getName());
            }
        } else {
            throw new BadRequestException("invalid.image.or.video.file");
        }
    }

    private String getReferences(List<String> references) {
        return Optional.ofNullable(references)
                .map(r -> String.join(",", r))
                .orElse(null);
    }

    private List<Long> getNotMutedFollowers(String authorizationHeader, Long personId, JsonStory story) {
        try {
            if (StoryAccessType.SELECTED_FOLLOWERS.equals(story.getAccessType())) {
                return new ArrayList<>(story.getSelectedFollowerIds());
            }
            RestPageImpl<JsonFollowing> followers = followingRestService.findSubscribers(authorizationHeader,
                    personId, 0, Integer.MAX_VALUE);

            return followers.stream().filter(f -> {
                try {
                    JsonFollowerNotificationSettings setting = personRestService.getFollowerNotificationSettings(authorizationHeader, f.getSubscriber().getId());
                    return setting == null || !setting.getIsMuted();
                } catch (Exception ex) {
                    return true;
                }
            }).map(f -> f.getSubscriber().getId()).toList();
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return new ArrayList<>();
    }
}
