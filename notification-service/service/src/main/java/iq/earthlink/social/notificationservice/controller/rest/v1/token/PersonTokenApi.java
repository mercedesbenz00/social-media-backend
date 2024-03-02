package iq.earthlink.social.notificationservice.controller.rest.v1.token;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import iq.earthlink.social.notificationservice.data.model.PersonToken;
import iq.earthlink.social.notificationservice.service.token.DefaultPersonTokenManager;
import iq.earthlink.social.security.DefaultSecurityProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Api(value = "PersonTokenApi", tags = "Person Token Api")
@RestController
@RequestMapping(value = "/api/v1/tokens", produces = MediaType.APPLICATION_JSON_VALUE)
public class PersonTokenApi {

    private final DefaultPersonTokenManager defaultPersonTokenManager;
    private final DefaultSecurityProvider securityProvider;

    public PersonTokenApi(DefaultPersonTokenManager defaultPersonTokenManager, DefaultSecurityProvider securityProvider) {
        this.defaultPersonTokenManager = defaultPersonTokenManager;
        this.securityProvider = securityProvider;
    }

    @GetMapping(value = "/person-device-tokens")
    @ApiOperation("Returns all user's tokens")
    public Page<PersonToken> getPersonTokensForCurrentUser(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "device", required = false) String device,
            Pageable pageable
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        if (device == null){
            return defaultPersonTokenManager.getPushTokensByPersonId(personId, pageable);
        } else {
            return defaultPersonTokenManager.getPushTokensByPersonIdAndDevice(personId, device, pageable);
        }
    }

    @PutMapping(value = "/person-device-tokens")
    @ApiOperation("Registers device token for current user identified by id_token")
    public PersonToken addPersonToken(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("pushToken") String pushToken,
            @RequestParam(value = "device") String device) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return defaultPersonTokenManager.addPushToken(personId, pushToken, device);
    }

    @DeleteMapping(value = "/person-device-tokens")
    @ApiOperation("Deletes device token for current user identified by id_token")
    @ApiResponses(
            @ApiResponse(message = "Count of deleted rows (1)", code = 200, response = int.class)
    )
    public void deletePersonToken(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("pushToken") String pushToken) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        defaultPersonTokenManager.deletePushToken(personId, pushToken);
    }
}
