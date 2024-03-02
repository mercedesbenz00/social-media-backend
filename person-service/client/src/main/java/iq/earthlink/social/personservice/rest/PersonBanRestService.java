package iq.earthlink.social.personservice.rest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign wrapper interface for getting information from person service about banned users.
 */
@FeignClient(name="person-service", url = "${person.service.url}")
public interface PersonBanRestService {

    @GetMapping(value = "/api/v1/persons/bans/groupBans/exist")
    boolean isPersonBannedFromGroup(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestParam("groupId") Long groupId, @RequestParam("personId") Long personId);
}
