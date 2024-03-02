package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.personservice.person.MuteManager;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.JsonPersonMute;
import iq.earthlink.social.personservice.security.SecurityProvider;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Mute Api", value = "MuteApi")
@RestController
@RequestMapping(value = "/api/v1/persons/{personId}/mutes", produces = MediaType.APPLICATION_JSON_VALUE)
public class MuteApi {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuteApi.class);

  private final MuteManager muteManager;
  private final PersonManager personManager;
  private final Mapper mapper;
  private final SecurityProvider securityProvider;

  public MuteApi(
          MuteManager muteManager,
          PersonManager personManager, Mapper mapper, SecurityProvider securityProvider) {
    this.muteManager = muteManager;
    this.personManager = personManager;
    this.mapper = mapper;
    this.securityProvider = securityProvider;
  }

  @ApiOperation("Returns the list of the mutes created by the target person")
  @GetMapping
  public Page<JsonPersonMute> findMutes(@PathVariable Long personId, Pageable page) {
    LOGGER.debug("Received 'find mutes' request for person: {} with pagination: {}",
        personId, page);

    return muteManager.findMutes(personManager.getPersonByIdInternal(personId), page)
            .map(m -> mapper.map(m, JsonPersonMute.class));
  }

  @ApiOperation("Creates new mute against target person")
  @PostMapping
  public JsonPersonMute createMute(
          @RequestHeader("Authorization") String authorizationHeader,
          @PathVariable Long personId) {
    Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
    Person currentUser = personManager.getPersonByIdInternal(currentUserId);
    LOGGER.debug("Received 'create mute' request for person: {} by owner: {}",
        personId, currentUser.getId());

    return mapper.map(muteManager.createMute(currentUser, personManager.getPersonByIdInternal(personId)), JsonPersonMute.class);
  }

  @ApiOperation("Removes the mute against target person")
  @DeleteMapping
  public void deleteMute(
          @RequestHeader("Authorization") String authorizationHeader,
          @PathVariable Long personId) {
    Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
    LOGGER.debug("Received 'delete mute' request for person: {} from owner: {}",
        personId, currentUserId);
    muteManager.removeMute(currentUserId, personManager.getPersonByIdInternal(personId));
  }
}
