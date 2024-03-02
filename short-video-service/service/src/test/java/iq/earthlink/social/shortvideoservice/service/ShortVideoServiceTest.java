package iq.earthlink.social.shortvideoservice.service;

import feign.FeignException;
import feign.Request;
import feign.Response;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioFileStorage;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.security.config.ServerAuthProperties;
import iq.earthlink.social.shortvideoregistryservice.dto.ShortVideoCategoryDTO;
import iq.earthlink.social.shortvideoregistryservice.dto.ShortVideoConfigurationDTO;
import iq.earthlink.social.shortvideoregistryservice.dto.ShortVideoDTO;
import iq.earthlink.social.shortvideoregistryservice.dto.UpdateShortVideoRequestDTO;
import iq.earthlink.social.shortvideoregistryservice.rest.ShortVideoRegistryRestService;
import iq.earthlink.social.shortvideoservice.model.*;
import iq.earthlink.social.shortvideoservice.utils.SecurityContextUtils;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static iq.earthlink.social.classes.enumeration.PrivacyLevel.PUBLIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;


class ShortVideoServiceTest {

    private static final String MOCK_TOKEN = "JWT_TOKEN";

    private static final String API_SECRET_KEY = "secret";
    private static final String SPORT = "Sport";

    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @InjectMocks
    private ShortVideoService shortVideoService;

    @Mock
    FileStorageProvider fileStorageProvider;

    @Mock
    MinioFileStorage fileStorage;

    @Mock
    private ShortVideoRegistryRestService shortVideoRegistryRestService;

    @Mock
    private SecurityContextUtils securityContextUtils;

    @Mock
    private ServerAuthProperties authProperties;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void whenSetShortVideoConfiguration_registryFeignServiceTriggered() {
        //given
        ShortVideoConfigurationRequestDTO requestDTO = ShortVideoConfigurationRequestDTO
                .builder()
                .privacyLevel(PUBLIC)
                .build();
        ShortVideoConfigurationDTO shortVideoConfigurationDTO = mapper.map(requestDTO, ShortVideoConfigurationDTO.class);
        shortVideoConfigurationDTO.setPersonId(1L);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ShortVideoConfigurationDTO> configurationCaptor = ArgumentCaptor.forClass(ShortVideoConfigurationDTO.class);
        willDoNothing().given(shortVideoRegistryRestService).setShortVideoConfiguration(any(String.class), any(ShortVideoConfigurationDTO.class));
        given(securityContextUtils.getAuthorizationToken()).willReturn(MOCK_TOKEN);
        given(securityContextUtils.getCurrentPersonId()).willReturn(1L);
        //when
        shortVideoService.setShortVideoConfiguration(requestDTO);
        //then
        then(shortVideoRegistryRestService).should().setShortVideoConfiguration(tokenCaptor.capture(), configurationCaptor.capture());
        assertThat(tokenCaptor.getValue()).isEqualTo(MOCK_TOKEN);
        assertThat(configurationCaptor.getValue()).isEqualTo(shortVideoConfigurationDTO);
    }

    @Test
    void whenRegistryRestServiceThrowsError_SetShortVideoConfigurationWillThrowRestApiException() {
        //given
        PersonInfo personInfo = JsonPersonProfile.builder().id(1L).build();
        ShortVideoConfigurationRequestDTO requestDTO = ShortVideoConfigurationRequestDTO
                .builder()
                .privacyLevel(PUBLIC)
                .build();
        willThrow(FeignException.errorStatus(
                "setShortVideoConfiguration",
                Response.builder()
                        .status(503)
                        .request(Request.create(Request.HttpMethod.GET, "", Map.of(), (byte[]) null, null, null))
                        .headers(new HashMap<>())
                        .reason("Connection refused").build())).given(shortVideoRegistryRestService).setShortVideoConfiguration(any(String.class), any(ShortVideoConfigurationDTO.class));
        given(securityContextUtils.getAuthorizationToken()).willReturn(MOCK_TOKEN);
        given(securityContextUtils.getCurrentPersonInfo()).willReturn(personInfo);
        //when
        //then
        assertThatThrownBy(() -> shortVideoService.setShortVideoConfiguration(requestDTO))
                .isInstanceOf(RestApiException.class)
                .hasCauseInstanceOf(FeignException.class);
        then(shortVideoRegistryRestService).should().setShortVideoConfiguration(any(String.class), any(ShortVideoConfigurationDTO.class));
    }

    @Test
    void whenUpdateShortVideo_registryFeignServiceTriggered() {
        //given
        String mockUrl = "http://localhost:9000/social-media-short-videos/948496b9-332b-4ed6-8c83-fbdda5b5c8a9";
        PersonInfo personInfo = JsonPersonProfile.builder().id(1L).build();
        UUID videoId = UUID.randomUUID();

        UpdateShortVideoDTO requestDTO = UpdateShortVideoDTO
                .builder()
                .privacyLevel(PUBLIC)
                .title("Updated Title")
                .categories(List.of(UpdateShortVideoCategoriesDTO.builder().name(SPORT).build()))
                .build();

        ShortVideoDTO expected = ShortVideoDTO.builder()
                .id(videoId)
                .title("Updated Title")
                .bucket("social-media-short-videos")
                .categories(Set.of(ShortVideoCategoryDTO.builder().name(SPORT).build()))
                .privacyLevel(PUBLIC)
                .build();

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UpdateShortVideoRequestDTO> configurationCaptor = ArgumentCaptor.forClass(UpdateShortVideoRequestDTO.class);
        willReturn(expected).given(shortVideoRegistryRestService).updateShortVideo(any(String.class), any(UUID.class), any(UpdateShortVideoRequestDTO.class));
        given(securityContextUtils.getAuthorizationToken()).willReturn(MOCK_TOKEN);
        given(securityContextUtils.getCurrentPersonInfo()).willReturn(personInfo);
        when(fileStorageProvider.getStorage(StorageType.MINIO)).thenReturn(fileStorage);
        when(fileStorage.getPresignedUrl(any(String.class), any(String.class))).thenReturn(mockUrl);
        //when
        ShortVideoResponseDTO responseDTO = shortVideoService.updateShortVideo(videoId, requestDTO);
        //then
        then(shortVideoRegistryRestService).should().updateShortVideo(tokenCaptor.capture(), eq(videoId), configurationCaptor.capture());
        assertThat(tokenCaptor.getValue()).isEqualTo(MOCK_TOKEN);
        assertThat(configurationCaptor.getValue().getTitle()).isEqualTo(expected.getTitle());
        assertThat(responseDTO.getUrl()).isEqualTo(mockUrl);
        assertThat(responseDTO.getId()).isEqualTo(videoId);
    }


    @Test
    void whenGetShortVideoById_returnVideoFromRegistryService() {
        //given
        PersonInfo personInfo = JsonPersonProfile.builder().id(1L).build();
        String mockUrl = "http://localhost:9000/social-media-short-videos/948496b9-332b-4ed6-8c83-fbdda5b5c8a9";
        UUID videoId = UUID.randomUUID();
        ShortVideoDTO expected = ShortVideoDTO.builder()
                .id(videoId)
                .title("Title")
                .bucket("social-media-short-videos")
                .categories(Set.of(ShortVideoCategoryDTO.builder().name(SPORT).build()))
                .privacyLevel(PUBLIC)
                .build();
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        willReturn(expected).given(shortVideoRegistryRestService).findShortVideoById(MOCK_TOKEN, videoId);
        given(securityContextUtils.getAuthorizationToken()).willReturn(MOCK_TOKEN);
        given(securityContextUtils.getCurrentPersonInfo()).willReturn(personInfo);
        when(fileStorageProvider.getStorage(StorageType.MINIO)).thenReturn(fileStorage);
        when(fileStorage.getPresignedUrl(any(String.class), any(String.class))).thenReturn(mockUrl);
        //when
        ShortVideoResponseDTO responseDTO = shortVideoService.findShortVideoById(videoId);
        //then
        then(shortVideoRegistryRestService).should().findShortVideoById(tokenCaptor.capture(), eq(videoId));
        assertThat(tokenCaptor.getValue()).isEqualTo(MOCK_TOKEN);
        assertThat(responseDTO.getUrl()).isEqualTo(mockUrl);
        assertThat(responseDTO.getId()).isEqualTo(videoId);
    }

    @Test
    void whenRegistryRestServiceThrowsError_GetShortVideoByIdWillThrowRestApiException() {
        //given
        UUID videoId = UUID.randomUUID();
        PersonInfo personInfo = JsonPersonProfile.builder().id(1L).build();
        willThrow(FeignException.errorStatus(
                "findShortVideoById",
                Response.builder()
                        .status(503)
                        .request(Request.create(Request.HttpMethod.GET, "", Map.of(), (byte[]) null, null, null))
                        .headers(new HashMap<>())
                        .reason("Connection refused").build())).given(shortVideoRegistryRestService).findShortVideoById(MOCK_TOKEN, videoId);
        given(securityContextUtils.getAuthorizationToken()).willReturn(MOCK_TOKEN);
        given(securityContextUtils.getCurrentPersonInfo()).willReturn(personInfo);
        //when
        //then
        assertThatThrownBy(() -> shortVideoService.findShortVideoById(videoId))
                .isInstanceOf(RestApiException.class)
                .hasCauseInstanceOf(FeignException.class);
        then(shortVideoRegistryRestService).should().findShortVideoById(MOCK_TOKEN, videoId);
    }

    @Test
    void whenUploadShortVideo_validateRequiredInput() {
        UploadShortVideoDTO requestBody = UploadShortVideoDTO.builder()
                .url("")
                .build();

        // given
        given(authProperties.getApiSecretKey()).willReturn(API_SECRET_KEY);

        // Empty API key:
        assertThatThrownBy(() -> shortVideoService.uploadShortVideo(null, requestBody))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.check.not.null");

        // Invalid API key:
        assertThatThrownBy(() -> shortVideoService.uploadShortVideo("", requestBody))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.api.key.invalid");

        assertThatThrownBy(() -> shortVideoService.uploadShortVideo("invalid key", requestBody))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.api.key.invalid");

        // Empty URL:
        assertThatThrownBy(() -> shortVideoService.uploadShortVideo(API_SECRET_KEY, requestBody))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.required.parameter.empty");

        // Empty title:
        requestBody.url("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4");
        requestBody.title("");
        assertThatThrownBy(() -> shortVideoService.uploadShortVideo(API_SECRET_KEY, requestBody))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.required.parameter.empty");

        // Invalid URL:
        requestBody.url("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.jpeg");
        requestBody.title("Short video title");
        assertThatThrownBy(() -> shortVideoService.uploadShortVideo(API_SECRET_KEY, requestBody))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.video.url.invalid");
    }

}