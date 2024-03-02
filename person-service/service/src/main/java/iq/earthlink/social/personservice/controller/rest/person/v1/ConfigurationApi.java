package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.personservice.controller.CurrentUser;
import iq.earthlink.social.personservice.dto.PersonDTO;
import iq.earthlink.social.personservice.person.rest.JsonPersonConfiguration;
import iq.earthlink.social.personservice.service.PersonConfigurationService;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@RestController
@RequestMapping("/api/v1/persons/config")
public class ConfigurationApi {

    private final PersonConfigurationService personConfigurationService;

    public ConfigurationApi(
        PersonConfigurationService personConfigurationService) {
        this.personConfigurationService = personConfigurationService;
    }

    @GetMapping
    @ApiOperation("Return person configuration for current user")
    public JsonPersonConfiguration getPersonConfiguration(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @CurrentUser PersonDTO person) {
        return personConfigurationService.getByPersonId(authorizationHeader, person.getId());
    }

    @PutMapping
    @ApiOperation("Updates person configuration for current user")
    public void updatePersonConfiguration(
        @RequestHeader(value = "Authorization") String authorizationHeader,
        @RequestBody @ApiParam("New configuration")
        JsonPersonConfiguration personConfiguration, @CurrentUser PersonDTO person) {

        personConfigurationService.updatePersonConfig(authorizationHeader, person.getId(), personConfiguration);
    }
}
