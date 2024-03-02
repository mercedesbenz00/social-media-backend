package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.personservice.person.FollowManager;
import iq.earthlink.social.personservice.person.FollowSearchCriteria;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.JsonFollowing;
import iq.earthlink.social.personservice.security.SecurityProvider;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Follow Api", value = "FollowApi")
@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class FollowerApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(FollowerApi.class);

    private final PersonManager personManager;
    private final FollowManager followManager;
    private final Mapper mapper;
    private final SecurityProvider securityProvider;

    public FollowerApi(PersonManager personManager, FollowManager followManager,
                       Mapper mapper, SecurityProvider securityProvider) {
        this.personManager = personManager;
        this.followManager = followManager;
        this.mapper = mapper;
        this.securityProvider = securityProvider;
    }

    /**
     * Creates following relation between persons.
     *
     * @param personId    the target person to be followed
     */
    @ApiOperation("Sets current person as a follower of the target person")
    @PostMapping("/persons/{personId}/follow")
    public JsonFollowing followPerson(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long personId) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);
        LOGGER.debug("Received 'follow' request from: {} for the followed person: {}",
                currentUser.getId(), personId);

        if (currentUser.getId().equals(personId)) {
            throw new BadRequestException("error.unable.follow.yourself");
        }

        final Person followedPerson = personManager.getPersonByIdInternal(personId);
        LOGGER.debug("Found person: {} to be followed by {}", followedPerson.getId(), currentUser.getId());

        return mapper.map(followManager.follow(currentUser, followedPerson), JsonFollowing.class);
    }

    /**
     * Removes following relation between user.
     *
     * @param personId    the target followed person
     */
    @ApiOperation("Removes following relationship between current person and the target person")
    @DeleteMapping("/persons/{personId}/follow")
    public void unfollowPerson(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long personId) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        LOGGER.debug("Received 'unfollow' request from person: {} to unfollow person: {}",
                currentUserId, personId);
        followManager.unfollow(currentUserId, personManager.getPersonByIdInternal(personId));
    }

    /**
     * Finds subscribers (followers) for the person.
     *
     * @param personId the target person id
     * @param page     the pagination params for the result set
     */
    @ApiOperation("Returns the list of followers for the target person ")
    @GetMapping("/persons/{personId}/followers")
    public Page<JsonFollowing> findSubscribers(
            @PathVariable Long personId,
            @RequestParam(value = "followerIds", required = false ) Long[] followerIds,
            @ApiParam("The query parameter filters followers by display name or first/last name")
            @RequestParam(value = "query", required = false) String query,
            Pageable page) {
        LOGGER.debug("Received 'find followers' request for the person: {} with pagination: {}",
                personId, page);

        FollowSearchCriteria criteria = FollowSearchCriteria.builder()
                .personId(personId)
                .followerIds(followerIds)
                .query(query)
                .build();

        return followManager.findFollowersOld(criteria, page).map(f -> mapper.map(f, JsonFollowing.class));
    }

    /**
     * Finds person subscriptions.
     *
     * @param personId the target person id
     * @param page     the pagination params for the result set
     */
    @ApiOperation("Returns the list of follow relationships where target person is the follower")
    @GetMapping("/persons/{personId}/following")
    public Page<JsonFollowing> findSubscriptions(
            @PathVariable Long personId,
            @RequestParam(value = "followingIds", required = false) Long[] followingIds,
            @RequestParam(value = "query", required = false) String query,
            Pageable page) {
        LOGGER.debug("Received 'find following persons' request for the person: {} with pagination: {}",
                personId, page);

        FollowSearchCriteria criteria = FollowSearchCriteria.builder()
                .personId(personId)
                .query(query)
                .followingIds(followingIds)
                .build();

        return followManager.findFollowedPersonsOld(criteria, page)
                .map(f -> mapper.map(f, JsonFollowing.class));
    }
}
