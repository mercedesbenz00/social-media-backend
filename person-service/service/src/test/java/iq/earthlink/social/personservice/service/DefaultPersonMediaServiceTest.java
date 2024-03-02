package iq.earthlink.social.personservice.service;

import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.file.MediaFileRepository;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioFileStorage;
import iq.earthlink.social.common.filestorage.minio.MinioProperties;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.personservice.person.impl.DefaultPersonMediaService;
import iq.earthlink.social.personservice.person.impl.repository.PersonRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.security.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class DefaultPersonMediaServiceTest {

    private static final String ADMIN = "ADMIN";
    private static final String TEST_FILE = "test file";

    @InjectMocks
    private DefaultPersonMediaService personMediaService;

    @Mock
    private PersonRepository personRepository;
    @Mock
    private MediaFileRepository fileRepository;
    @Mock
    private FileStorageProvider fileStorageProvider;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Mock
    private MinioFileStorage minioFileStorage;

    @Mock
    private MinioProperties minioProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void uploadAvatar_userNotProvided_throwException() {
        Person currentUser = getUser(1L, ADMIN);
        MultipartFile imageFile = getImageFile();

        assertThatThrownBy(() -> personMediaService.uploadAvatar(null, imageFile, currentUser))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.check.not.null");
    }

    @Test
    void uploadAvatar_userNotAllowed_throwException() {
        Person currentUser = getUser(1L, ADMIN);
        Person user = getUser(2L, "USER");
        MultipartFile imageFile = getImageFile();

        assertThatThrownBy(() -> personMediaService.uploadAvatar(currentUser, imageFile, user))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.person.can.not.modify.avatar.or.cover");
    }

    @Test
    void uploadAvatar_invalidImageFile_throwException() {
        Person currentUser = getUser(1L, ADMIN);
        MultipartFile videoFile = getVideoFile();

        assertThatThrownBy(() -> personMediaService.uploadAvatar(currentUser, videoFile, currentUser))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("invalid.image.file");
    }

    @Test
    void uploadAvatar_saveImage_returnSavedObject() {
        Person currentUser = getUser(1L, ADMIN);
        MultipartFile imageFile = getImageFile();

        MediaFile fileRecord = MediaFile.builder()
                .ownerId(1L)
                .fileType(MediaFileType.AVATAR)
                .mimeType(imageFile.getContentType())
                .storageType(StorageType.MINIO)
                .size(imageFile.getSize())
                .build();

        Person updatedUser = Person.builder().avatar(fileRecord).build();

        given(fileRepository.findByOwnerIdAndFileType(1L, MediaFileType.AVATAR)).willReturn(null);
        given(fileRepository.save(any(MediaFile.class))).willReturn(fileRecord);
        given(personRepository.save(currentUser)).willReturn(updatedUser);
        given(fileStorageProvider.getStorage(StorageType.MINIO)).willReturn(minioFileStorage);
        given(minioProperties.getEndpoint()).willReturn("https://minio:9000");
        given(minioProperties.getAccessKey()).willReturn("access_key");
        given(minioProperties.getSecretKey()).willReturn("secret_key");

        MediaFile uploaded = personMediaService.uploadAvatar(currentUser, imageFile, currentUser);
        assertEquals(updatedUser.getAvatar(), uploaded);
    }

    private Person getUser(Long id, String role) {
        Role roleObj = new Role();
        roleObj.setId(id);
        roleObj.setCode(role);
        return Person.builder()
                .id(id)
                .email("test1@abc.com")
                .username("abc1234_" + id)
                .displayName("abc1234_" + id)
                .uuid(UUID.randomUUID())
                .isVerifiedAccount(true)
                .personRoles(Set.of(roleObj))
                .build();
    }

    private MockMultipartFile getImageFile() {
        return new MockMultipartFile(TEST_FILE, TEST_FILE, "image/", new byte[]{});
    }

    private MockMultipartFile getVideoFile() {
        return new MockMultipartFile(TEST_FILE, TEST_FILE, "video/", new byte[]{});
    }
}



