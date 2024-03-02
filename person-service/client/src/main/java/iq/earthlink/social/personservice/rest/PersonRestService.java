package iq.earthlink.social.personservice.rest;

import iq.earthlink.social.personservice.person.rest.JsonFollowerNotificationSettings;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "person-service", url = "${person.service.url}")
public interface PersonRestService {

    @GetMapping(path = "/api/v1/persons/by-id/{personId}")
    JsonPerson getPersonById(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable("personId") Long personId);

    @GetMapping(path = "/internal/persons/{personId}")
    JsonPersonProfile getPersonByIdInternal(
            @RequestHeader("X-API-KEY") String apiKey,
            @PathVariable("personId") Long personId);

    @GetMapping(path = "/api/v1/persons/profile")
    JsonPersonProfile getPersonProfile(@RequestHeader(value = "Authorization") String authorizationHeader);

    @GetMapping(value = "/api/v1/persons/notification-settings/{followerId}")
    JsonFollowerNotificationSettings getFollowerNotificationSettings(@RequestHeader(value = "Authorization") String authorizationHeader,
                                                                     @PathVariable("followerId") Long followerId);

    @GetMapping(value = "/api/v1/persons/notification-settings/muted-following")
    List<Long> getPersonIdsWhoMutedFollowingId();
}
