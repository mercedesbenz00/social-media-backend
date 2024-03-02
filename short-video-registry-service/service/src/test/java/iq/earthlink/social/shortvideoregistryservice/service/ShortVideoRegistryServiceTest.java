package iq.earthlink.social.shortvideoregistryservice.service;

import com.fasterxml.uuid.Generators;
import com.google.common.collect.Sets;
import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.shortvideoregistryservice.dto.*;
import iq.earthlink.social.shortvideoregistryservice.model.*;
import iq.earthlink.social.shortvideoregistryservice.repository.*;
import iq.earthlink.social.shortvideoregistryservice.util.SecurityContextUtils;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;


class ShortVideoRegistryServiceTest {

    private static final String CATEGORY_UUID_1 = "768e7399-b775-4511-bc66-d4f44460682a";
    private static final String CATEGORY_UUID_2 = "bcabb959-1557-450f-be7f-760d0b10c4ea";
    private static final String SHORT_VIDEO_UUID = "4c7746ea-3f1f-4133-8f74-51449c2a97bb";
    private static final String BUCKET = "social-media-short-videos";
    private static final String FROM_DATE = "2022-08-01";

    @Spy
    private Mapper mapper = DozerBeanMapperBuilder
            .create()
            .withMappingBuilder(new BeanMappingBuilder() {
                @Override
                protected void configure() {
                    mapping(Category.class,
                            ShortVideoCategoryDTO.class)
                            .fields("id", "categoryId");
                }
            })
            .build();

    @Mock
    private ShortVideoConfigurationRepository shortVideoConfigurationRepository;

    @Mock
    private ShortVideoRepository shortVideoRepository;

    @Mock
    private ShortVideosByAuthorRepository shortVideosByAuthorRepository;

    @Mock
    ShortVideosOfCategoryRepository shortVideosOfCategoryRepository;

    @Mock
    ShortVideoStatsRepository shortVideoStatsRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SecurityContextUtils securityContextUtils;

    @InjectMocks
    private ShortVideoRegistryService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void whenNullInputParameter_throwException() {
        //given
        ShortVideoConfigurationDTO configurationDTO = null;
        //when
        //then
        assertThatThrownBy(() -> service.setShortVideoConfiguration(configurationDTO))
                .isInstanceOf(RestApiException.class);
        then(shortVideoConfigurationRepository).should(never()).save(any(ShortVideoConfiguration.class));
    }

    @Test
    void whenSelectedUsersPrivacyLevelAndEmptyUserList_throwForbiddenException() {
        //given
        ShortVideoConfigurationDTO configurationDTO = ShortVideoConfigurationDTO
                .builder()
                .personId(1L)
                .privacyLevel(PrivacyLevel.SELECTED_USERS)
                .build();
        //when
        //then
        assertThatThrownBy(() -> service.setShortVideoConfiguration(configurationDTO))
                .isInstanceOf(BadRequestException.class);
        then(shortVideoConfigurationRepository).should(never()).save(any(ShortVideoConfiguration.class));
    }

    @Test
    void whenSelectedGroupsPrivacyLevelAndEmptyGroupsList_throwForbiddenException() {
        //given
        ShortVideoConfigurationDTO configurationDTO = ShortVideoConfigurationDTO
                .builder()
                .personId(1L)
                .privacyLevel(PrivacyLevel.SELECTED_GROUPS)
                .build();
        //when
        //then
        assertThatThrownBy(() -> service.setShortVideoConfiguration(configurationDTO))
                .isInstanceOf(BadRequestException.class);
        then(shortVideoConfigurationRepository).should(never()).save(any(ShortVideoConfiguration.class));
    }

    @Test
    void whenNoExistingConfigurationInDB_createNewConfigurationEntityAndSave() {
        //given
        Long personId = 1L;
        ShortVideoConfigurationDTO configurationDTO = ShortVideoConfigurationDTO
                .builder()
                .personId(personId)
                .privacyLevel(PrivacyLevel.PUBLIC)
                .build();
        ArgumentCaptor<ShortVideoConfiguration> configurationArgumentCaptor =
                ArgumentCaptor.forClass(ShortVideoConfiguration.class);
        given(shortVideoConfigurationRepository.findByPersonId(personId)).willReturn(Optional.empty());
        //when
        service.setShortVideoConfiguration(configurationDTO);
        //then
        then(shortVideoConfigurationRepository).should().save(configurationArgumentCaptor.capture());
        ShortVideoConfiguration configurationToSave = configurationArgumentCaptor.getValue();
        assertThat(configurationToSave.getPersonId()).isEqualTo(personId);
        assertThat(configurationToSave.getPrivacyLevel()).isEqualTo(PrivacyLevel.PUBLIC);
    }

    @Test
    void whenSelectedUsersListExist_saveConfigurationsWithProvidedList() {
        //given
        Long personId = 1L;
        List<Long> selectedUsers = List.of(2L, 3L, 4L);
        ShortVideoConfigurationDTO configurationDTO = ShortVideoConfigurationDTO
                .builder()
                .personId(personId)
                .privacyLevel(PrivacyLevel.SELECTED_USERS)
                .selectedUsers(selectedUsers)
                .build();
        ArgumentCaptor<ShortVideoConfiguration> configurationArgumentCaptor =
                ArgumentCaptor.forClass(ShortVideoConfiguration.class);
        given(shortVideoConfigurationRepository.findByPersonId(personId)).willReturn(Optional.empty());
        //when
        service.setShortVideoConfiguration(configurationDTO);
        //then
        then(shortVideoConfigurationRepository).should().save(configurationArgumentCaptor.capture());
        ShortVideoConfiguration configurationToSave = configurationArgumentCaptor.getValue();
        assertThat(configurationToSave.getPrivacyLevel()).isEqualTo(PrivacyLevel.SELECTED_USERS);
        assertThat(configurationToSave.getSelectedUsers()).isNotEmpty();
        assertThat(configurationToSave.getSelectedUsers()).isEqualTo(Set.copyOf(selectedUsers));
    }

    @Test
    void whenSelectedGroupListProvided_saveConfigurationsWithProvidedList() {
        //given
        Long personId = 1L;
        List<Long> selectedGroups = List.of(2L, 3L, 4L);
        ShortVideoConfigurationDTO configurationDTO = ShortVideoConfigurationDTO
                .builder()
                .personId(personId)
                .privacyLevel(PrivacyLevel.SELECTED_GROUPS)
                .selectedGroups(selectedGroups)
                .build();
        ArgumentCaptor<ShortVideoConfiguration> configurationArgumentCaptor =
                ArgumentCaptor.forClass(ShortVideoConfiguration.class);
        given(shortVideoConfigurationRepository.findByPersonId(personId)).willReturn(Optional.empty());
        //when
        service.setShortVideoConfiguration(configurationDTO);
        //then
        then(shortVideoConfigurationRepository).should().save(configurationArgumentCaptor.capture());
        ShortVideoConfiguration configurationToSave = configurationArgumentCaptor.getValue();
        assertThat(configurationToSave.getPrivacyLevel()).isEqualTo(PrivacyLevel.SELECTED_GROUPS);
        assertThat(configurationToSave.getSelectedGroups()).isNotEmpty();
        assertThat(configurationToSave.getSelectedGroups()).isEqualTo(Set.copyOf(selectedGroups));
    }

    @Test
    void whenUpdatingWithPublicPrivacyLevel_UsersListInEntityShouldBeEmpty() {
        //given
        Long personId = 1L;
        Set<Long> mockUserList = Sets.newHashSet(2L, 3L, 4L);
        ShortVideoConfigurationDTO configurationDTO = ShortVideoConfigurationDTO
                .builder()
                .personId(personId)
                .privacyLevel(PrivacyLevel.PUBLIC)
                .build();
        ShortVideoConfiguration shortVideoConfiguration = ShortVideoConfiguration
                .builder()
                .selectedUsers(mockUserList)
                .build();
        ArgumentCaptor<ShortVideoConfiguration> configurationArgumentCaptor =
                ArgumentCaptor.forClass(ShortVideoConfiguration.class);
        given(shortVideoConfigurationRepository.findByPersonId(personId)).willReturn(Optional.of(shortVideoConfiguration));
        //when
        service.setShortVideoConfiguration(configurationDTO);
        //then
        then(shortVideoConfigurationRepository).should().save(configurationArgumentCaptor.capture());
        ShortVideoConfiguration configurationToSave = configurationArgumentCaptor.getValue();
        assertThat(configurationToSave.getPrivacyLevel()).isEqualTo(PrivacyLevel.PUBLIC);
        assertThat(configurationToSave.getSelectedUsers()).isEmpty();
    }

    @Test
    void whenUpdatingWithPublicPrivacyLevel_GroupsListInEntityShouldBeEmpty() {
        //given
        Long personId = 1L;
        Set<Long> mockGroupsList = Sets.newHashSet(2L, 3L, 4L);
        ShortVideoConfigurationDTO configurationDTO = ShortVideoConfigurationDTO
                .builder()
                .personId(personId)
                .privacyLevel(PrivacyLevel.PUBLIC)
                .build();
        ShortVideoConfiguration shortVideoConfiguration = ShortVideoConfiguration
                .builder()
                .selectedGroups(mockGroupsList)
                .build();
        ArgumentCaptor<ShortVideoConfiguration> configurationArgumentCaptor =
                ArgumentCaptor.forClass(ShortVideoConfiguration.class);
        given(shortVideoConfigurationRepository.findByPersonId(personId)).willReturn(Optional.of(shortVideoConfiguration));
        //when
        service.setShortVideoConfiguration(configurationDTO);
        //then
        then(shortVideoConfigurationRepository).should().save(configurationArgumentCaptor.capture());
        ShortVideoConfiguration configurationToSave = configurationArgumentCaptor.getValue();
        assertThat(configurationToSave.getPrivacyLevel()).isEqualTo(PrivacyLevel.PUBLIC);
        assertThat(configurationToSave.getSelectedGroups()).isEmpty();
    }

    @Test
    void whenCommentsAllowedProvided_setCommentAllowedInEntity() {
        //given
        Long personId = 1L;
        ShortVideoConfigurationDTO configurationDTO = ShortVideoConfigurationDTO
                .builder()
                .personId(personId)
                .privacyLevel(PrivacyLevel.PUBLIC)
                .commentsAllowed(false)
                .build();
        ShortVideoConfiguration shortVideoConfiguration = ShortVideoConfiguration
                .builder()
                .commentsAllowed(true)
                .build();
        ArgumentCaptor<ShortVideoConfiguration> configurationArgumentCaptor =
                ArgumentCaptor.forClass(ShortVideoConfiguration.class);
        given(shortVideoConfigurationRepository.findByPersonId(personId)).willReturn(Optional.of(shortVideoConfiguration));
        //when
        service.setShortVideoConfiguration(configurationDTO);
        //then
        then(shortVideoConfigurationRepository).should().save(configurationArgumentCaptor.capture());
        ShortVideoConfiguration configurationToSave = configurationArgumentCaptor.getValue();
        assertThat(configurationToSave.getPrivacyLevel()).isEqualTo(PrivacyLevel.PUBLIC);
        assertThat(configurationToSave.getCommentsAllowed()).isFalse();
    }

    @Test
    void getShortVideoConfigurations_shouldReturnNotNullResponse() {
        //given
        Long personId = 1L;
        ShortVideoConfiguration shortVideoConfiguration = ShortVideoConfiguration
                .builder()
                .commentsAllowed(true)
                .personId(personId)
                .build();
        given(securityContextUtils.getCurrentPersonId()).willReturn(personId);
        given(shortVideoConfigurationRepository.findByPersonId(personId)).willReturn(Optional.of(shortVideoConfiguration));
        //when
        ShortVideoConfigurationDTO shortVideoConfigurationDTO = service.getShortVideoConfiguration();
        //then
        assertThat(shortVideoConfigurationDTO).isNotNull();
        assertThat(shortVideoConfigurationDTO.getPersonId()).isEqualTo(personId);
    }

    @Test
    void getShortVideoConfigurationsFirstTime_shouldCreateDefaultConfigurationAndReturnIt() {
        //given
        Long personId = 1L;
        given(securityContextUtils.getCurrentPersonId()).willReturn(personId);
        given(shortVideoConfigurationRepository.findByPersonId(personId)).willReturn(Optional.empty());
        given(shortVideoConfigurationRepository.save(any(ShortVideoConfiguration.class))).willAnswer(i -> i.getArguments()[0]);
        //when
        ShortVideoConfigurationDTO shortVideoConfigurationDTO = service.getShortVideoConfiguration();
        //then
        assertThat(shortVideoConfigurationDTO).isNotNull();
        assertThat(shortVideoConfigurationDTO.getPersonId()).isEqualTo(personId);
        assertThat(shortVideoConfigurationDTO.getPrivacyLevel()).isEqualTo(PrivacyLevel.PUBLIC);
        assertThat(shortVideoConfigurationDTO.getCommentsAllowed()).isTrue();
    }

    @Test
    void getShortVideoById_shouldReturnNotNullResponse() {
        //given
        UUID id = Generators.timeBasedGenerator().generate();
        ShortVideo shortVideo = ShortVideo.builder()
                .id(id)
                .privacyLevel(PrivacyLevel.PUBLIC)
                .commentsAllowed(true)
                .build();
        given(shortVideoRepository.findById(id)).willReturn(Optional.of(shortVideo));
        //when
        ShortVideoDTO shortVideoDTO = service.findShortVideoById(id);
        //then
        assertThat(shortVideoDTO).isNotNull();
        assertThat(shortVideoDTO.getId()).isEqualTo(id);
        assertThat(shortVideoDTO.getPrivacyLevel()).isEqualTo(PrivacyLevel.PUBLIC);
        assertThat(shortVideoDTO.getCommentsAllowed()).isTrue();
    }

    @Test
    void findShortVideoById_shouldReturnValidResponse() {
        //given
        UUID id = Generators.timeBasedGenerator().generate();
        ShortVideo shortVideo = ShortVideo.builder()
                .id(id)
                .privacyLevel(PrivacyLevel.PUBLIC)
                .commentsAllowed(true)
                .build();
        ShortVideoStats stats = ShortVideoStats.builder()
                .id(id)
                .likes(1L)
                .comments(2L)
                .build();
        given(shortVideoRepository.findById(id)).willReturn(Optional.of(shortVideo));
        given(shortVideoStatsRepository.findById(id)).willReturn(Optional.of(stats));

        //when
        ShortVideoDTO shortVideoDTO = service.findShortVideoById(id);
        //then
        assertThat(shortVideoDTO).isNotNull();
        assertThat(shortVideoDTO.getId()).isEqualTo(id);
        assertThat(shortVideoDTO.getPrivacyLevel()).isEqualTo(PrivacyLevel.PUBLIC);
        assertThat(shortVideoDTO.getCommentsAllowed()).isTrue();
    }

    @Test
    void whenWrongShortVideoId_throwException() {
        //given
        UUID id = Generators.timeBasedGenerator().generate();
        //when
        //then
        assertThatThrownBy(() -> service.findShortVideoById(id)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("error.video.not.found", id.toString());
    }

    @Test
    void whenFindShortVideosByAuthorId_returnFilteredList() {
        given(shortVideosByAuthorRepository
                .findByAuthorIdAndYearAndCreatedAtGreaterThanEqual(eq(2L), eq(2022), eq(DateUtil.getDateFromString(FROM_DATE)), any(Pageable.class)))
                .willReturn(Optional.of(getExpectedShortVideosByAuthor()));

        //when
        CassandraPageDTO<ShortVideoDTO> shortVideoDTOs = service.findShortVideosByAuthor(2L,
                FROM_DATE, PageInfo.builder().pagingState(null).size(1).build());

        //then
        assertThat(shortVideoDTOs.getContent()).allMatch(v -> v.getAuthorId().equals(2L));
        assertThat(shortVideoDTOs.getCount()).isEqualTo(2);
    }

    private Slice<ShortVideoByAuthor> getExpectedShortVideosByAuthor() {
        List<ShortVideoByAuthor> shortVideoByAuthors = new ArrayList<>(List.of(
                ShortVideoByAuthor
                        .builder()
                        .id(UUID.fromString("4c7746ea-3f1f-4133-8f74-51449c2a97ba"))
                        .title("Short Video By Author")
                        .bucket(BUCKET)
                        .authorId(2L)
                        .commentsAllowed(true)
                        .privacyLevel(PrivacyLevel.SELECTED_USERS)
                        .selectedUsers(Set.of(2L, 3L))
                        .selectedGroups(Set.of())
                        .categories(Set.of(UUID.fromString(CATEGORY_UUID_1)))
                        .build(),
                ShortVideoByAuthor
                        .builder()
                        .id(UUID.fromString(SHORT_VIDEO_UUID))
                        .title("Another short video by author")
                        .bucket(BUCKET)
                        .authorId(2L)
                        .commentsAllowed(false)
                        .privacyLevel(PrivacyLevel.SELECTED_GROUPS)
                        .selectedUsers(Set.of())
                        .selectedGroups(Set.of(1L, 2L))
                        .categories(Set.of(UUID.fromString(CATEGORY_UUID_1)))
                        .build()
        ));
        return new PageImpl<>(shortVideoByAuthors);
    }

    @Test
    void whenFindShortVideosByCategories_returnFilteredList() {
        ShortVideoStats stats = ShortVideoStats.builder()
                .id(UUID.fromString(CATEGORY_UUID_2))
                .likes(1L)
                .comments(2L)
                .build();
        given(shortVideoStatsRepository.findById(any(UUID.class))).willReturn(Optional.of(stats));

        //given
        given(shortVideosOfCategoryRepository
                .findByCategoryIdInAndYearAndCreatedAtGreaterThanEqual(
                        anyCollection(), eq(2022), any(Timestamp.class), any(CassandraPageRequest.class)))
                .willReturn(Optional.of(getExpectedShortVideosByCategories()));
        given(shortVideoRepository
                .findById(UUID.fromString(SHORT_VIDEO_UUID)))
                .willReturn(Optional.of(ShortVideo
                        .builder()
                        .categories(Set.of(UUID.fromString(CATEGORY_UUID_1), UUID.fromString(CATEGORY_UUID_2)))
                        .build()));

        given(categoryRepository.findByIdIn(Set.of(UUID.fromString(CATEGORY_UUID_1), UUID.fromString(CATEGORY_UUID_2))))
                .willReturn(Optional.of(List.of(
                        Category
                                .builder()
                                .name("Sport")
                                .id(UUID.fromString(CATEGORY_UUID_1))
                                .build(),
                        Category
                                .builder()
                                .name("Sport")
                                .id(UUID.fromString(CATEGORY_UUID_2))
                                .build())));
        //when
        CassandraPageDTO<ShortVideoDTO> shortVideoDTOs = service.findShortVideosByCategory(List.of(UUID.fromString(CATEGORY_UUID_1), UUID.fromString(CATEGORY_UUID_2)),
                FROM_DATE, PageInfo.builder().pagingState(null).size(1).build());

        assertEquals(shortVideoDTOs.getContent().size(), getExpectedShortVideosByCategories().getContent().size());
        shortVideoDTOs.getContent().forEach(
                video -> assertThat(video.getCategories().stream().map(ShortVideoCategoryDTO::getCategoryId).collect(Collectors.toList()))
                        .containsAnyElementsOf(List.of(UUID.fromString(CATEGORY_UUID_1), UUID.fromString(CATEGORY_UUID_2))));

    }

    private Slice<ShortVideosOfCategory> getExpectedShortVideosByCategories() {
        List<ShortVideosOfCategory> shortVideoByCategories = new ArrayList<>();
        shortVideoByCategories.add(
                ShortVideosOfCategory
                        .builder()
                        .id(UUID.fromString(SHORT_VIDEO_UUID))
                        .title("Short Video By Categories")
                        .bucket(BUCKET)
                        .authorId(1L)
                        .commentsAllowed(false)
                        .privacyLevel(PrivacyLevel.SELECTED_GROUPS)
                        .selectedUsers(Set.of())
                        .selectedGroups(Set.of(1L, 2L))
                        .categories(Set.of(UUID.fromString(CATEGORY_UUID_1), UUID.fromString(CATEGORY_UUID_2)))
                        .build()
        );
        return new PageImpl<>(shortVideoByCategories);
    }
}