package iq.earthlink.social.shortvideoservice.pact;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import iq.earthlink.social.shortvideoregistryservice.dto.CassandraPageDTO;
import iq.earthlink.social.shortvideoregistryservice.dto.ShortVideoCategoryDTO;
import iq.earthlink.social.shortvideoregistryservice.dto.ShortVideoConfigurationDTO;
import iq.earthlink.social.shortvideoregistryservice.dto.ShortVideoDTO;
import iq.earthlink.social.shortvideoregistryservice.rest.ShortVideoRegistryRestService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactTestFor(providerName = "short-video-registry-service-producer", port = "8091")
@SpringBootTest(properties = {
        // overriding provider address
        "addresses.ribbon.listOfServers: localhost:8091"
})
@Disabled
public class ShortVideoRegistryServiceConsumerPactTest {

    public static final String GET_SHORT_VIDEO_PATH = "/api/v1/short-video/";
    public static final UUID VIDEO_ID = UUID.randomUUID();
    private static final String COMMENTS_ALLOWED = "commentsAllowed";
    private static final String PRIVACY_LEVEL = "privacyLevel";
    private static final String SELECTED_USERS_LEVEL = "SELECTED_USERS";
    private static final String SELECTED_USERS = "selectedUsers";
    private static final String SELECTED_GROUPS = "selectedGroups";
    private static final String BEARER_JWT_TOKEN = "Bearer JWT_TOKEN";
    private static final String REGEX_PATTERN = "\\d.*";
    private static final String AUTHOR_ID = "authorId";
    private static final String FROM_DATE = "2022-08-01";
    private static final String CATEGORY_UUID_1 = "768e7399-b775-4511-bc66-d4f44460682a";
    private static final String CATEGORY_UUID_2 = "bcabb959-1557-450f-be7f-760d0b10c4ea";
    private static final String SHORT_VIDEO_UUID = "4c7746ea-3f1f-4133-8f74-51449c2a97bb";
    private static final String CATEGORIES = "categories";
    private static final String SPORT = "Sport";
    private static final String CATEGORY_ID = "categoryId";

    @Autowired
    private ShortVideoRegistryRestService shortVideoRegistryRestService;

    @Pact(consumer = "short-video-service-consumer", provider = "short-video-registry-service-producer")
    RequestResponsePact getUserConfiguration(PactDslWithProvider builder) {
        return builder.given("configuration exists")
                .uponReceiving("get user short video configuration")
                .method("GET")
                .path("/api/v1/short-video/configuration")
                .willRespondWith()
                .status(200)
                .headers(headers())
                .body(newJsonBody((object) ->
                        object.numberType("personId", 1L)
                                .booleanType(COMMENTS_ALLOWED)
                                .stringType(PRIVACY_LEVEL, SELECTED_USERS_LEVEL)
                                .array(SELECTED_USERS, selectedUsersArr -> selectedUsersArr.integerType(2L).integerType(3L))
                                .minArrayLike(SELECTED_GROUPS, 0, PactDslJsonRootValue.integerType(0), 0)
                ).build())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getUserConfiguration")
    void getShortVideoConfiguration_whenConfigurationExist() {
        ShortVideoConfigurationDTO expected = ShortVideoConfigurationDTO
                .builder()
                .privacyLevel(PrivacyLevel.SELECTED_USERS)
                .commentsAllowed(true)
                .personId(1L)
                .selectedUsers(List.of(2L, 3L))
                .selectedGroups(List.of())
                .build();
        ShortVideoConfigurationDTO shortVideoConfigurationDTO = shortVideoRegistryRestService.getShortVideoConfiguration(BEARER_JWT_TOKEN);

        assertThat(expected).isEqualTo(shortVideoConfigurationDTO);
    }


    @Pact(consumer = "short-video-service-consumer", provider = "short-video-registry-service-producer")
    RequestResponsePact findShortVideosByAuthor(PactDslWithProvider builder) {
        return builder
                .given("short videos by author")
                .uponReceiving("find short videos by author ID")
                .method("GET")
                .path("/api/v1/short-video/by-author")
                .matchQuery(AUTHOR_ID, REGEX_PATTERN, "1")
                .matchQuery("fromDate", "\\d{4}-\\d{2}-\\d{2}", FROM_DATE)
                .matchQuery("size", REGEX_PATTERN, "1")
                .willRespondWith()
                .status(200)
                .headers(headers())
                .body(getShortVideosByAuthor())
                .toPact();
    }

    @Pact(consumer = "short-video-service-consumer", provider = "short-video-registry-service-producer")
    RequestResponsePact findShortVideosByCategories(PactDslWithProvider builder) {
        return builder
                .given("short videos by categories")
                .uponReceiving("find short videos by categories")
                .method("GET")
                .path("/api/v1/short-video/by-categories")
                .matchQuery("categoryIds", "(.+?)(?:,|$)", List.of(CATEGORY_UUID_1, CATEGORY_UUID_2))
                .matchQuery("fromDate", "\\d{4}-\\d{2}-\\d{2}", FROM_DATE)
                .matchQuery("size", REGEX_PATTERN, "1")
                .willRespondWith()
                .status(200)
                .headers(headers())
                .body(getShortVideosByCategories())
                .toPact();
    }

    @Pact(consumer = "short-video-service-consumer", provider = "short-video-registry-service-producer")
    RequestResponsePact findShortVideoById(PactDslWithProvider builder) {
        return builder
                .given("short video by id")
                .uponReceiving("find short video by ID")
                .method("GET")
                .path(GET_SHORT_VIDEO_PATH + UUID.fromString(SHORT_VIDEO_UUID))
                .willRespondWith()
                .status(200)
                .headers(headers())
                .body(getShortVideoById())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "findShortVideosByAuthor")
    void findShortVideosByAuthor_returnsHttpStatusOk() {
        CassandraPageDTO<ShortVideoDTO> expected = getExpectedShortVideosByAuthor();

        // Find videos by author ID:
        CassandraPageDTO<ShortVideoDTO> shortVideos = shortVideoRegistryRestService.findShortVideosByAuthor(BEARER_JWT_TOKEN, 1L, FROM_DATE, 1, null);
        assertEquals(shortVideos.getContent().size(), expected.getContent().size());
        assertThat(shortVideos.getContent().get(0))
                .isEqualToIgnoringGivenFields(expected.getContent().get(0), CATEGORIES);
        assertThat(shortVideos.getContent().get(0).getCategories())
                .usingFieldByFieldElementComparator()
                .containsAll(expected.getContent().get(0).getCategories());
    }


    private CassandraPageDTO<ShortVideoDTO> getExpectedShortVideosByAuthor() {

        CassandraPageDTO<ShortVideoDTO> expected = new CassandraPageDTO<>();
        expected.setCount(1);
        expected.setHasNext(false);
        expected.setPagingState(null);
        expected.setContent(Collections.singletonList(ShortVideoDTO
                .builder()
                .id(UUID.fromString("4c7746ea-3f1f-4133-8f74-51449c2a97ba"))
                .title("Short Video By Author")
                .bucket("social-media-short-videos")
                .authorId(1L)
                .commentsAllowed(true)
                .privacyLevel(PrivacyLevel.SELECTED_USERS)
                .selectedUsers(Set.of(2L, 3L))
                .selectedGroups(Set.of())
                .categories(Set.of(ShortVideoCategoryDTO.builder()
                        .name(SPORT)
                        .categoryId(UUID.fromString(CATEGORY_UUID_1))
                        .build()))
                .build()));
        return expected;
    }

    @Test
    @PactTestFor(pactMethod = "findShortVideosByCategories")
    void findShortVideosByCategories_returnsHttpStatusOk() {
        // Find videos by categories:
        CassandraPageDTO<ShortVideoDTO> shortVideos = shortVideoRegistryRestService
                .findShortVideosByCategories(BEARER_JWT_TOKEN, List.of(CATEGORY_UUID_1, CATEGORY_UUID_2), FROM_DATE, 1, null);
        assertThat(shortVideos.getContent()).hasSize(1);
    }

    @Test
    @PactTestFor(pactMethod = "findShortVideoById")
    void findShortVideoById_returnsHttpStatusOk() {
        List<ShortVideoDTO> expected = createShortVideoList();

        // Find video by ID:
        ShortVideoDTO shortVideo = shortVideoRegistryRestService.findShortVideoById(BEARER_JWT_TOKEN, UUID.fromString(SHORT_VIDEO_UUID));
        assertThat(expected.get(0).getAuthorId()).isEqualTo(shortVideo.getAuthorId());
        assertThat(expected.get(0).getPrivacyLevel()).isEqualTo(shortVideo.getPrivacyLevel());
    }

    private List<ShortVideoDTO> createShortVideoList() {
        List<ShortVideoDTO> expected = new ArrayList<>();
        expected.add(createShortVideo(1L, PrivacyLevel.SELECTED_USERS, Set.of(2L, 3L), Set.of(), true));
        expected.add(createShortVideo(1L, PrivacyLevel.SELECTED_GROUPS, Set.of(), Set.of(1L, 2L), false));
        return expected;
    }

    private ShortVideoDTO createShortVideo(Long authorId, PrivacyLevel privacyLevel, Set<Long> selectedUsers, Set<Long> selectedGroups, boolean commentsAllowed) {
        return ShortVideoDTO
                .builder()
                .privacyLevel(privacyLevel)
                .commentsAllowed(commentsAllowed)
                .authorId(authorId)
                .selectedUsers(selectedUsers)
                .selectedGroups(selectedGroups)
                .build();
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private DslPart getShortVideosByAuthor() {
        return newJsonBody((object) -> object
                .numberType("count", 1L)
                .booleanType("hasNext", false)
                .stringType("pagingState", null)
                .array("content", contentArr -> contentArr
                        .object(content -> content
                                .uuid("id", UUID.fromString("4c7746ea-3f1f-4133-8f74-51449c2a97ba"))
                                .numberType(AUTHOR_ID, 1L)
                                .stringType("title", "Short Video By Author")
                                .stringType("bucket", "social-media-short-videos")
                                .stringType(PRIVACY_LEVEL, SELECTED_USERS_LEVEL)
                                .array(SELECTED_USERS, selectedUsersArr -> selectedUsersArr.integerType(2L).integerType(3L))
                                .stringType(SELECTED_GROUPS, null)
                                .booleanType(COMMENTS_ALLOWED, true)
                                .array(CATEGORIES, categoryIdsArr ->
                                        categoryIdsArr.object(category ->
                                                category.stringType(CATEGORY_ID, CATEGORY_UUID_1)
                                                        .stringType("name", SPORT))
                                )
                        )
                )
        ).build();
    }

    private DslPart getShortVideosByCategories() {
        return newJsonBody((object) ->
                object
                        .numberType("count", 1L)
                        .booleanType("hasNext", true)
                        .stringType("pagingState", null)
                        .array("content", contentArr ->
                                contentArr.object(
                                        content -> content
                                                .uuid("id", UUID.fromString(SHORT_VIDEO_UUID))
                                                .numberType(AUTHOR_ID, 1L)
                                                .stringType(PRIVACY_LEVEL, "SELECTED_GROUPS")
                                                .array(SELECTED_GROUPS, selectedUsersArr -> selectedUsersArr.integerType(1L).integerType(2L))
                                                .booleanType(COMMENTS_ALLOWED, false)
                                                .array(CATEGORIES, categoryIdsArr -> categoryIdsArr
                                                        .object(category ->
                                                                category.stringType(CATEGORY_ID, CATEGORY_UUID_1)
                                                                        .stringType("name", SPORT))
                                                        .object(category ->
                                                                category.stringType(CATEGORY_ID, CATEGORY_UUID_2)
                                                                        .stringType("name", "Movie")))
                                )
                        )
        ).build();

    }

    private DslPart getShortVideoById() {
        return newJsonBody((object) ->
                object.uuid("id", UUID.fromString(SHORT_VIDEO_UUID))
                        .numberType(AUTHOR_ID, 1L)
                        .stringType(PRIVACY_LEVEL, SELECTED_USERS_LEVEL)
                        .array(SELECTED_USERS, selectedUsersArr -> selectedUsersArr.integerType(2L).integerType(3L))
                        .booleanType(COMMENTS_ALLOWED, false)
                        .array(CATEGORIES, categoryIdsArr ->
                                categoryIdsArr
                                        .object(category ->
                                                category
                                                        .stringType(CATEGORY_ID, CATEGORY_UUID_1)
                                                        .stringType("name", SPORT))
                                        .object(category ->
                                                category
                                                        .stringType(CATEGORY_ID, CATEGORY_UUID_2)
                                                        .stringType("name", "Movie")))
        ).build();
    }
}
