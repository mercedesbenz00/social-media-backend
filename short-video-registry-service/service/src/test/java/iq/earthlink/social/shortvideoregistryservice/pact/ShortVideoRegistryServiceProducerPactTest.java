package iq.earthlink.social.shortvideoregistryservice.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.shortvideoregistryservice.model.*;
import iq.earthlink.social.shortvideoregistryservice.repository.*;
import iq.earthlink.social.shortvideoregistryservice.util.SecurityContextUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@Provider("short-video-registry-service-producer")
@IgnoreNoPactsToVerify
@PactBroker
@ActiveProfiles({"api", "test"})
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, CassandraAutoConfiguration.class})
@Disabled
class ShortVideoRegistryServiceProducerPactTest {

    private static final String CATEGORY_UUID_1 = "768e7399-b775-4511-bc66-d4f44460682a";
    private static final String CATEGORY_UUID_2 = "bcabb959-1557-450f-be7f-760d0b10c4ea";
    private static final String SHORT_VIDEO_UUID = "4c7746ea-3f1f-4133-8f74-51449c2a97bb";
    private static final String SPORT = "Sport";

    @LocalServerPort
    int port;

    @Autowired
    private ShortVideoConfigurationRepository repository;

    @Autowired
    private ShortVideoRepository shortVideoRepository;

    @Autowired
    private ShortVideosByAuthorRepository shortVideosByAuthorRepository;

    @Autowired
    private ShortVideosOfCategoryRepository shortVideosOfCategoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SecurityContextUtils securityContextUtils;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        if (Objects.nonNull(context))
            context.setTarget(new HttpTestTarget("localhost", port));
    }

    @BeforeAll
    static void enablePublishingPact() {
        System.setProperty("pact.verifier.publishResults", "true");
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verifyPact(PactVerificationContext context) {
        if (Objects.nonNull(context))
            context.verifyInteraction();
    }

    @State("configuration exists")
    void toConfigurationExistsState() {
        given(securityContextUtils.getCurrentPersonInfo()).willReturn(
                JsonPersonProfile.builder().id(1L).displayName("Admin").build()
        );
        given(repository.findByPersonId(any(Long.class))).willReturn(
                Optional.of(ShortVideoConfiguration
                        .builder()
                        .privacyLevel(PrivacyLevel.SELECTED_USERS)
                        .personId(1L)
                        .commentsAllowed(true)
                        .selectedUsers(Set.of(2L, 3L))
                        .selectedGroups(Set.of())
                        .build()));
    }

    @State("short videos by author")
    void shortVideosByAuthor() {
        given(shortVideosByAuthorRepository
                .findByAuthorIdAndYearAndCreatedAtGreaterThanEqual(eq(1L), eq(2022), eq(DateUtil.getDateFromString("2022-08-01")), any(Pageable.class)))
                .willReturn(Optional.of(getExpectedShortVideosByAuthor()));

        given(shortVideoRepository
                .findById(UUID.fromString("4c7746ea-3f1f-4133-8f74-51449c2a97ba")))
                .willReturn(Optional.of(ShortVideo
                        .builder()
                        .categories(Set.of(UUID.fromString(CATEGORY_UUID_1)))
                        .build()));

        given(categoryRepository.findByIdIn(Set.of(UUID.fromString(CATEGORY_UUID_1))))
                .willReturn(Optional.of(List.of(Category
                        .builder()
                        .name(SPORT)
                        .id(UUID.fromString(CATEGORY_UUID_1))
                        .build())));

    }


    @State("short videos by categories")
    void shortVideosByCategories() {
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
                                .name(SPORT)
                                .id(UUID.fromString(CATEGORY_UUID_1))
                                .build(),
                        Category
                                .builder()
                                .name("Movie")
                                .id(UUID.fromString(CATEGORY_UUID_2))
                                .build())));

    }

    @State("short video by id")
    void shortVideoById() {
        given(shortVideoRepository
                .findById(UUID.fromString(SHORT_VIDEO_UUID)))
                .willReturn(Optional.of(ShortVideo
                        .builder()
                        .id(UUID.fromString(SHORT_VIDEO_UUID))
                        .authorId(1L)
                        .privacyLevel(PrivacyLevel.SELECTED_USERS)
                        .selectedGroups(Set.of())
                        .selectedUsers(Set.of(2L, 3L))
                        .commentsAllowed(false)
                        .categories(Set.of(UUID.fromString(CATEGORY_UUID_1), UUID.fromString(CATEGORY_UUID_2)))
                        .build()));

        given(categoryRepository.findByIdIn(Set.of(UUID.fromString(CATEGORY_UUID_1), UUID.fromString(CATEGORY_UUID_2))))
                .willReturn(Optional.of(List.of(
                        Category
                                .builder()
                                .name(SPORT)
                                .id(UUID.fromString(CATEGORY_UUID_1))
                                .build(),
                        Category
                                .builder()
                                .name("Movie")
                                .id(UUID.fromString(CATEGORY_UUID_2))
                                .build())));

    }

    private Slice<ShortVideoByAuthor> getExpectedShortVideosByAuthor() {
        List<ShortVideoByAuthor> shortVideoByAuthors = new ArrayList<>();
        shortVideoByAuthors.add(
                ShortVideoByAuthor
                        .builder()
                        .id(UUID.fromString("4c7746ea-3f1f-4133-8f74-51449c2a97ba"))
                        .title("Short Video By Author")
                        .bucket("social-media-short-videos")
                        .authorId(1L)
                        .commentsAllowed(true)
                        .privacyLevel(PrivacyLevel.SELECTED_USERS)
                        .selectedUsers(Set.of(2L, 3L))
                        .selectedGroups(Set.of())
                        .categories(Set.of(UUID.fromString(CATEGORY_UUID_1)))
                        .build()
        );
        return new PageImpl<>(shortVideoByAuthors);
    }

    private Slice<ShortVideosOfCategory> getExpectedShortVideosByCategories() {
        List<ShortVideosOfCategory> shortVideoByCategories = new ArrayList<>();
        shortVideoByCategories.add(
                ShortVideosOfCategory
                        .builder()
                        .id(UUID.fromString(SHORT_VIDEO_UUID))
                        .title("Short Video By Categories")
                        .bucket("social-media-short-videos")
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
