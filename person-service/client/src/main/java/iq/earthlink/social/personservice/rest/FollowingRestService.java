package iq.earthlink.social.personservice.rest;

import iq.earthlink.social.common.rest.RestPageImpl;
import iq.earthlink.social.personservice.person.rest.JsonFollowing;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "person-service", url = "${person.service.url}")
public interface FollowingRestService {

    @GetMapping(path = "/api/v1/persons/{personId}/following")
    RestPageImpl<JsonFollowing> findSubscriptions(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable(value = "personId") Long forPersonId,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size);

    @GetMapping(path = "/api/v1/persons/{personId}/followers")
    RestPageImpl<JsonFollowing> findSubscribers(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable(value = "personId") Long forPersonId,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size);

    @GetMapping(path = "/api/v2/persons/{personId}/following")
    RestPageImpl<JsonPerson> findSubscriptionsV2(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable("personId") Long personId,
            @RequestParam("followingIds") Long[] followingIds,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size);
}
