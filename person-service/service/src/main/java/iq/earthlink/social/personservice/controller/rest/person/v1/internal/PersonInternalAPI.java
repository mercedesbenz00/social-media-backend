package iq.earthlink.social.personservice.controller.rest.person.v1.internal;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.personservice.config.ServerAuthProperties;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Person Internal Api", value = "PersonInternalAPI")
@RestController
@RequestMapping(value = "/internal", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class PersonInternalAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonInternalAPI.class);
    private final PersonManager personManager;
    private ServerAuthProperties authProperties;

    public PersonInternalAPI(PersonManager personManager) {
        this.personManager = personManager;
    }


    @ApiOperation("Return all active persons")
    @GetMapping("/persons/{personId}")
    public JsonPersonProfile getPersonById(
            @RequestHeader("X-API-KEY") String apiKey,
            @PathVariable Long personId) {
        LOGGER.debug("Internal request to get person by id: {}", personId);
        if (!StringUtils.equals(authProperties.getApiAuthHeader(), apiKey)) {
            throw new ForbiddenException("error.api.key.invalid");
        }
        return personManager.getPersonByPersonIdInternal(personId);
    }
}
