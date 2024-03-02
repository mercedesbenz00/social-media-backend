package iq.earthlink.social.personservice.controller.rest.person.v2;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.personservice.person.FollowManager;
import iq.earthlink.social.personservice.person.FollowSearchCriteria;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.personservice.security.SecurityProvider;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "Follow Api", value = "FollowApi")
@RestController
@RequestMapping(value = "/api/v2", produces = MediaType.APPLICATION_JSON_VALUE)
public class FollowerApiV2 {

  private static final Logger LOGGER = LoggerFactory.getLogger(FollowerApiV2.class);

  private final PersonManager personManager;
  private final FollowManager followManager;
  private final Mapper mapper;
  private final SecurityProvider securityProvider;

  public FollowerApiV2(PersonManager personManager, FollowManager followManager,
                       Mapper mapper, SecurityProvider securityProvider) {
    this.personManager = personManager;
    this.followManager = followManager;
    this.mapper = mapper;
    this.securityProvider = securityProvider;
  }

  /**
   * Creates following relation between persons.
   *
   * @param personId the target person to be followed
   */
  @ApiOperation("Sets current person as a follower of the target person")
  @PostMapping("/persons/{personId}/follow")
  public JsonPerson followPerson(
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
    followManager.follow(currentUser, followedPerson);
    return mapper.map(followedPerson, JsonPerson.class);
  }

  /**
   * Removes following relation between user.
   *
   * @param personId the target followed person
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
   * @param page the pagination params for the result set
   */
  @ApiOperation("Returns the list of followers for the target person ")
  @GetMapping("/persons/{personId}/followers")
  public Page<JsonPerson> findSubscribers(
      @PathVariable Long personId,
      @RequestParam(value = "followerIds", required = false ) Long[] followerIds,
      @ApiParam("The query parameter filters followers by display name or first/last name")
      @RequestParam(value = "query", required = false) String query,
      Pageable page) {
    LOGGER.debug("Received 'find followers' request for the person: {} with pagination: {}",
        personId, page);

    FollowSearchCriteria criteria = FollowSearchCriteria.builder()
        .personId(personId)
        .query(query)
        .followerIds(followerIds)
        .build();

    return followManager.findFollowers(criteria, page);
  }

  /**
   * Finds person subscriptions.
   *
   * @param personId the target person id
   * @param page the pagination params for the result set
   */
  @ApiOperation("Returns the list of follow relationships where target person is the follower")
  @GetMapping("/persons/{personId}/following")
  public Page<JsonPerson> findSubscriptions(
      @PathVariable Long personId,
      @RequestParam(value = "followingIds", required = false ) Long[] followingIds,
      @RequestParam(value = "query", required = false) String query,
      Pageable page) {
    LOGGER.debug("Received 'find following persons' request for the person: {} with pagination: {}",
        personId, page);

    FollowSearchCriteria criteria = FollowSearchCriteria.builder()
        .personId(personId)
        .query(query)
        .followingIds(followingIds)
        .build();

    return followManager.findFollowedPersons(criteria, page);
  }
  @ApiOperation("Returns the list of follow relationships where target person is the follower with notification settings")
  @GetMapping("/persons/{personId}/following-with-settings")
  public List<JsonPerson> findSubscriptionsWithNotificationSettings(
      @PathVariable Long personId,
      @RequestParam(value = "followingIds", required = false ) Long[] followingIds) {
    LOGGER.debug("Received 'find following persons with notification settings' request for the person: {}", personId);

    FollowSearchCriteria criteria = FollowSearchCriteria.builder()
        .personId(personId)
        .followingIds(followingIds)
        .build();

    return followManager.findFollowedPersons(criteria,  Pageable.unpaged()).getContent();
  }
}
