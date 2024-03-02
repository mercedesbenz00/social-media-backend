package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.classes.data.dto.JsonStoryConfiguration;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.story.StoryManager;
import iq.earthlink.social.postservice.story.rest.JsonStory;
import iq.earthlink.social.postservice.story.rest.JsonStoryView;
import iq.earthlink.social.postservice.story.view.StoryView;
import iq.earthlink.social.postservice.story.view.StoryViewManager;
import iq.earthlink.social.security.SecurityProvider;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Api(value = "StoryApi", tags = "Story Api")
@RestController
@RequestMapping("/api/v1/stories")
public class StoryApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoryApi.class);

    private final StoryManager storyManager;
    private final StoryViewManager storyViewManager;
    private final Mapper mapper;
    private final SecurityProvider securityProvider;

    public StoryApi(StoryManager storyManager,
                    StoryViewManager storyViewManager,
                    Mapper mapper,
                    SecurityProvider securityProvider) {
        this.storyManager = storyManager;
        this.storyViewManager = storyViewManager;
        this.mapper = mapper;
        this.securityProvider = securityProvider;
    }

    @ApiOperation("Creates a new story")
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonStory createStory(@RequestHeader("Authorization") String authorizationHeader,
                                 @RequestPart(required = false) List<String> references,
                                 @CurrentUser PersonDTO person,
                                 @ApiParam(value = "The media file for the story", required = true)
                                         MultipartFile file) {

        if (Objects.isNull(file)) {
            throw new BadRequestException("error.file.must.be.not.null");
        }

        LOGGER.debug("Requested create story with file: {} by {}", file.getName(), person.getPersonId());

        JsonStory story = storyManager.createStory(authorizationHeader, person, file, references);
        LOGGER.info("Created new story: {}", story);

        return story;
    }

    @ApiOperation("Returns the story found by id")
    @GetMapping("/{storyId}")
    public JsonStory getStory(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long storyId
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        LOGGER.debug("Requested get story: {} by {}", storyId, personId);

        return storyManager.getStory(storyId);
    }

    @ApiOperation("Set story configuration for the user")
    @PutMapping("/configuration")
    public void setStoryConfiguration(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid JsonStoryConfiguration configuration
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        LOGGER.debug("Requested to set story configuration for the person with Id: {}", personId);

        storyManager.setStoryConfiguration(personId, configuration);
    }

    @ApiOperation("Get story configuration for the user")
    @GetMapping("/configuration")
    public JsonStoryConfiguration getStoryConfiguration(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        LOGGER.debug("Requested to get story configuration for the person with Id: {}", personId);

        return mapper.map(storyManager.getStoryConfiguration(personId), JsonStoryConfiguration.class);
    }

    @ApiOperation("Returns the stories list matched by criteria")
    @GetMapping
    public Page<JsonStory> findStories(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Filters stories by IDs of person subscriptions, or person ID. If not specified, returns stories of all person subscriptions.")
            @RequestParam(required = false) List<Long> ownerIds,
            @ApiParam("Filters not viewed stories if set to true or omitted.")
            @RequestParam(defaultValue = "true") Boolean unseenOnly,
            Pageable page) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        LOGGER.debug("Requested find stories of authors: {} by {}", ownerIds, personId);

        return storyManager.findStories(authorizationHeader, personId, ownerIds, unseenOnly, page);
    }

    @ApiOperation(value = "Returns the story's media file content", response = InputStream.class)
    @GetMapping("/{storyId}/media")
    public ResponseEntity<Resource> downloadStoryMedia(
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            @PathVariable Long storyId
    ) {
        return storyManager.downloadMedia(rangeHeader, storyId);
    }

    @ApiOperation("Removes the story by id")
    @DeleteMapping("/{storyId}")
    public void removeStory(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long storyId) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        LOGGER.debug("Requested remove story: {} by {}", storyId, personId);
        storyManager.removeStory(personId, storyId);
    }

    @ApiOperation("Save the story view by the current user")
    @PutMapping("/{storyId}/views")
    public JsonStoryView saveView(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long storyId) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        StoryView storyView = storyViewManager.save(storyId, personId);
        return mapper.map(storyView, JsonStoryView.class);
    }

    @ApiOperation("Returns the full views list for the person")
    @GetMapping("/views")
    public List<JsonStoryView> getViews(@RequestHeader("Authorization") String authorizationHeader) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return storyViewManager.findViews(personId)
                .stream()
                .map(v -> mapper.map(v, JsonStoryView.class))
                .toList();
    }
}
