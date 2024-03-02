package iq.earthlink.social.postservice.service;

import io.minio.MinioClient;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonStoryConfiguration;
import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.classes.enumeration.StoryAccessType;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.file.MediaFileRepository;
import iq.earthlink.social.common.filestorage.CompositeFileStorageProvider;
import iq.earthlink.social.common.filestorage.FileStorage;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioFileStorage;
import iq.earthlink.social.common.filestorage.minio.MinioProperties;
import iq.earthlink.social.common.rest.RestPageImpl;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.personservice.person.rest.JsonFollowing;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.personservice.person.rest.JsonPersonConfiguration;
import iq.earthlink.social.personservice.rest.FollowingRestService;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.postservice.event.KafkaProducerService;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.story.DefaultStoryManager;
import iq.earthlink.social.postservice.story.PostStoryProperties;
import iq.earthlink.social.postservice.story.StoryConfigurationRepository;
import iq.earthlink.social.postservice.story.StoryRepository;
import iq.earthlink.social.postservice.story.model.Story;
import iq.earthlink.social.postservice.story.model.StoryConfiguration;
import iq.earthlink.social.postservice.story.rest.JsonStory;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class DefaultStoryManagerTest {

    private static final String FILE_NAME = "test file";

    @InjectMocks
    private DefaultStoryManager storyManager;

    @Mock
    private MediaFileRepository fileRepository;
    @Mock
    private StoryRepository repository;
    @Mock
    private FileStorageProvider fileStorageProvider;
    @Mock
    private StoryConfigurationRepository configurationRepository;
    @Mock
    private PersonRestService personRestService;
    @Mock
    private FollowingRestService followingRestService;
    @Mock
    private PostStoryProperties properties;
    @Mock
    private MinioProperties minioProperties;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private MinioClient minioClient;
    @Mock
    private MinioFileStorage minioFileStorage;

    @MockBean
    private CachingConnectionFactory connectionFactory;
    @MockBean
    private CompositeFileStorageProvider compositeFileStorageProvider;

    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void createStory_invalidImageFile_throwException() {
        Long personId = 1L;

        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();

        MultipartFile imageFile = new MockMultipartFile(FILE_NAME, FILE_NAME, "image/", new byte[10]);
        // 40MB
        MultipartFile oversizedImageFile = new MockMultipartFile(FILE_NAME, FILE_NAME, "image/", new byte[41943041]);
        MultipartFile notAllowedFile = new MockMultipartFile(FILE_NAME, FILE_NAME, "file/", new byte[]{});

        MediaFile imageFileRecord = MediaFile.builder()
                .ownerId(1L)
                .fileType(MediaFileType.AVATAR)
                .mimeType(imageFile.getContentType())
                .storageType(StorageType.MINIO)
                .size(imageFile.getSize())
                .build();

        JsonMediaFile jsonMediaFile = JsonMediaFile.builder()
                .ownerId(1L)
                .fileType(MediaFileType.AVATAR)
                .mimeType(imageFile.getContentType())
                .size(imageFile.getSize())
                .build();

        Story imageStoryExpected = Story.builder()
                .id(1L)
                .accessType(StoryAccessType.PUBLIC)
                .authorId(person.getPersonId())
                .build();

        JsonStory jsonImageStoryExpected = JsonStory.builder()
                .id(1L)
                .accessType(StoryAccessType.PUBLIC)
                .authorId(person.getPersonId())
                .file(jsonMediaFile)
                .build();

        StoryConfiguration configuration = StoryConfiguration.builder()
                .id(1L)
                .accessType(StoryAccessType.PUBLIC)
                .personId(1L)
                .allowedFollowersIds(Set.of(2L, 3L))
                .build();

        JsonPersonConfiguration personConfiguration = JsonPersonConfiguration.builder()
                .personId(1L)
                .story(mapper.map(configuration, JsonStoryConfiguration.class))
                .notificationMute(true)
                .build();

        JsonFollowing follower = new JsonFollowing();
        follower.setId(2L);
        follower.setSubscriber(JsonPerson.builder().id(person.getPersonId()).build());
        RestPageImpl<JsonFollowing> followers = new RestPageImpl<>(Collections.singletonList(follower));

        given(fileRepository.save(any(MediaFile.class))).willReturn(imageFileRecord);
        given(properties.getImageMaxSize()).willReturn("10");
        given(properties.getVideoMaxSize()).willReturn("200");
        given(followingRestService.findSubscribers(null, personId, 0, Integer.MAX_VALUE)).willReturn(followers);
        given(repository.save(any(Story.class))).willReturn(imageStoryExpected);
        doNothing().when(kafkaProducerService).sendMessage(any(), any());
        when(fileStorageProvider.getStorage(StorageType.MINIO)).thenReturn(minioFileStorage);
        given(minioProperties.getEndpoint()).willReturn("https://minio:9000");
        given(minioProperties.getAccessKey()).willReturn("access_key");
        given(minioProperties.getSecretKey()).willReturn("secret_key");
        given(fileRepository.findByOwnerIdAndFileType(imageStoryExpected.getId(), MediaFileType.STORY)).willReturn(List.of(imageFileRecord));


        JsonStory imageStory = storyManager.createStory(null, person, imageFile, null);

        assertNotNull(imageStory);
        assertEquals(imageStory.getId(), jsonImageStoryExpected.getId());
        assertEquals(imageStory.getAuthorId(), jsonImageStoryExpected.getAuthorId());
        assertEquals(imageStory.getFile(), jsonImageStoryExpected.getFile());

        assertThatThrownBy(() -> storyManager.createStory(null, person, notAllowedFile, null))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("invalid.image.or.video.file");

        assertThatThrownBy(() -> storyManager.createStory(null, person, oversizedImageFile, null))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.file.upload.size.exceeded");
    }

    @Test
    void createStory_invalidVideoFile_throwException() {
        Long personId = 1L;

        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();

        MultipartFile videoFile = new MockMultipartFile(FILE_NAME, FILE_NAME, "video/", new byte[345656]);
        // 300MB
        MultipartFile oversizedVideoFile = new MockMultipartFile(FILE_NAME, FILE_NAME, "video/", new byte[257913583]);
        MultipartFile notAllowedFile = new MockMultipartFile(FILE_NAME, FILE_NAME, "file/", new byte[]{});

        MediaFile videoFileRecord = MediaFile.builder()
                .ownerId(1L)
                .fileType(MediaFileType.AVATAR)
                .mimeType(videoFile.getContentType())
                .storageType(StorageType.MINIO)
                .size(videoFile.getSize())
                .build();

        Story videoStoryExpected = Story.builder()
                .id(2L)
                .accessType(StoryAccessType.PUBLIC)
                .authorId(person.getPersonId())
                .build();

        JsonMediaFile jsonVideoFileRecord = JsonMediaFile.builder()
                .ownerId(1L)
                .fileType(MediaFileType.AVATAR)
                .mimeType(videoFile.getContentType())
                .size(videoFile.getSize())
                .build();

        JsonStory jsonVideoStoryExpected = JsonStory.builder()
                .id(2L)
                .accessType(StoryAccessType.PUBLIC)
                .authorId(person.getPersonId())
                .file(jsonVideoFileRecord)
                .build();

        StoryConfiguration configuration = StoryConfiguration.builder()
                .id(1L)
                .accessType(StoryAccessType.PUBLIC)
                .personId(1L)
                .allowedFollowersIds(Set.of(2L, 3L))
                .build();

        JsonPersonConfiguration personConfiguration = JsonPersonConfiguration.builder()
                .personId(1L)
                .story(mapper.map(configuration, JsonStoryConfiguration.class))
                .notificationMute(true)
                .build();
        JsonFollowing follower = new JsonFollowing();
        follower.setId(2L);
        follower.setSubscriber(JsonPerson.builder().id(person.getPersonId()).build());
        RestPageImpl<JsonFollowing> followers = new RestPageImpl<>(Collections.singletonList(follower));

        given(fileRepository.save(any(MediaFile.class))).willReturn(videoFileRecord);
        given(properties.getImageMaxSize()).willReturn("10");
        given(properties.getVideoMaxSize()).willReturn("200");
        given(followingRestService.findSubscribers(null, personId, 0, Integer.MAX_VALUE)).willReturn(followers);
        given(personRestService.getFollowerNotificationSettings(null, follower.getId())).willReturn(null);
        given(repository.save(any(Story.class))).willReturn(videoStoryExpected);

        when(fileStorageProvider.getStorage(StorageType.MINIO)).thenReturn(minioFileStorage);
        given(fileRepository.findByOwnerIdAndFileType(videoStoryExpected.getId(), MediaFileType.STORY)).willReturn(List.of(videoFileRecord));
        JsonStory videoStory = storyManager.createStory(null, person, videoFile, null);

        assertNotNull(videoStory);
        assertEquals(videoStory.getId(), jsonVideoStoryExpected.getId());
        assertEquals(videoStory.getAuthorId(), jsonVideoStoryExpected.getAuthorId());
        assertEquals(jsonVideoStoryExpected.getFile(), videoStory.getFile());

        assertThatThrownBy(() -> storyManager.createStory(null, person, notAllowedFile, null))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("invalid.image.or.video.file");

        assertThatThrownBy(() -> storyManager.createStory(null, person, oversizedVideoFile, null))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.file.upload.size.exceeded");

    }

    @Test
    void createStory_invalidFile_throwRestApiException() {
        // given
        Long personId = 1L;
        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();
        MultipartFile invalidFile = new MockMultipartFile(FILE_NAME, FILE_NAME, "text/", new byte[123]);
        MediaFile invalidFileRecord = MediaFile.builder()
                .ownerId(1L)
                .fileType(MediaFileType.AVATAR)
                .mimeType(invalidFile.getContentType())
                .storageType(StorageType.MINIO)
                .size(invalidFile.getSize())
                .build();

        Story story = Story.builder()
                .id(1L)
                .build();

        StoryConfiguration configuration = StoryConfiguration.builder()
                .id(1L)
                .accessType(StoryAccessType.PUBLIC)
                .personId(1L)
                .allowedFollowersIds(Set.of(2L, 3L))
                .build();

        JsonPersonConfiguration personConfiguration = JsonPersonConfiguration.builder()
                .personId(1L)
                .story(mapper.map(configuration, JsonStoryConfiguration.class))
                .notificationMute(true)
                .build();
        JsonFollowing follower = new JsonFollowing();
        follower.setId(2L);
        follower.setSubscriber(JsonPerson.builder().id(person.getPersonId()).build());
        RestPageImpl<JsonFollowing> followers = new RestPageImpl<>(Collections.singletonList(follower));

        given(followingRestService.findSubscribers(null, personId, 0, Integer.MAX_VALUE)).willReturn(followers);
        given(personRestService.getFollowerNotificationSettings(null, follower.getId())).willReturn(null);
        // when
        when(repository.save(any(Story.class))).thenReturn(story);
        when(fileRepository.save(any(MediaFile.class))).thenReturn(invalidFileRecord);

        // then
        assertThrows(RestApiException.class, () -> storyManager.createStory(null, person, invalidFile, null));

    }

    @Test
    void getStory_exist_success() {
        // given
        Long storyId = 1L;

        Story story = Story.builder()
                .id(storyId)
                .build();
        // when
        when(repository.findById(storyId)).thenReturn(Optional.of(story));

        // then
        JsonStory result = storyManager.getStory(storyId);
        assertEquals(storyId, result.getId());
    }

    @Test
    void getStory_invalidId_throwNotFoundException() {
        // given
        Long storyId = 0L;

        // when
        when(repository.findById(storyId)).thenReturn(Optional.empty());

        // then
        assertThrows(NotFoundException.class, () -> storyManager.getStory(storyId));

    }

    @Test
    void findStoriesExistingByPersonId_success() {
        // given
        Long personId = 1L;
        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();

        Story story = Story.builder()
                .id(1L)
                .authorId(personId)
                .build();


        // when
        when(followingRestService.findSubscriptions("", personId,
                0, Integer.MAX_VALUE)).thenReturn(new RestPageImpl<>(new ArrayList<>()));
        when(properties.getStoryLifetimeDays()).thenReturn("1");

        when(followingRestService.findSubscriptions("", personId,
                0, Integer.MAX_VALUE)).thenReturn(new RestPageImpl<>(new ArrayList<>()));
        when(followingRestService.findSubscribers("", personId,
                0, Integer.MAX_VALUE)).thenReturn(new RestPageImpl<>(new ArrayList<>()));
        when(repository.findAllowedStoriesAndSortUnseenFirst(eq(person.getPersonId()), eq(Set.of(1L)), eq(List.of()), any(Date.class), eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(story)));
        when(fileStorageProvider.getStorage(any(StorageType.class))).thenReturn(minioFileStorage);
        when(minioFileStorage.getUrl(anyString())).thenReturn("");

        // then
        Page<JsonStory> result = storyManager.findStories("", person.getPersonId(), List.of(personId), false, Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findStoriesByPersonIdNotExist_throwNotFoundException() {
        // given
        Long personId = 1L;
        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();

        Pageable unpaged = Pageable.unpaged();

        // when
        List<Long> personIds = List.of(personId);
        when(repository.findAllById(personIds)).thenReturn(Collections.emptyList());
        when(followingRestService.findSubscriptions("", personId,
                0, Integer.MAX_VALUE)).thenThrow(NotFoundException.class);

        // then
        assertThrows(NotFoundException.class, () -> storyManager.findStories("", person.getPersonId(), personIds, false, unpaged));

    }

    @Test
    void removeStory_success() {
        // given
        Long personId = 1L;
        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();

        FileStorage fileStorage = mock(FileStorage.class);
        // create a mock Story object
        Story story = Story
                .builder()
                .id(456L)
                .authorId(personId)
//                .file(MediaFile.builder().id(456L).ownerId(123L).storageType(StorageType.MINIO).build())
                .build();
        MediaFile mediaFile = MediaFile.builder().id(456L).ownerId(123L).storageType(StorageType.MINIO).build();
        // set up the mock objects
        when(repository.findById(456L)).thenReturn(Optional.of(story));
        when(fileRepository.findById(456L)).thenReturn(Optional.of(new MediaFile()));
        when(fileStorageProvider.getStorage(mediaFile.getStorageType())).thenReturn(fileStorage);
        doNothing().when(fileRepository).deleteById(456L);

        doNothing().when(repository).delete(story);

        // then
        storyManager.removeStory(person.getPersonId(), 456L);
        verify(repository, times(1)).delete(story);
    }

    @Test
    void removeStoryInvalidId_throwNotFoundException() {
        Long personId = 1L;
        PersonDTO person = PersonDTO.builder()
                .personId(personId)
                .build();
        Long storyId = 0L;
        // given
        when(repository.findById(storyId)).thenReturn(Optional.empty());

        // then
        assertThrows(NotFoundException.class, () -> storyManager.removeStory(person.getPersonId(), storyId));

    }

}
