package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.rest.UserStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The API controller responsible for managing {@link UserStats} resource.
 */
@Api(tags = "Statistics Api", value = "StatisticsApi")
@RestController
@RequestMapping(value = "/api/v1/stats")
public class UserStatisticsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserStatisticsApi.class);

    private final PersonManager personManager;

    public UserStatisticsApi(PersonManager personManager) {
        this.personManager = personManager;
    }

    @ApiOperation(
            value = "Returns user statistics for specified time period",
            response = UserStats.class)
    @ApiResponses({
            @ApiResponse(code = 500, message = "If any unexpected server error")
    })
    @GetMapping()
    public ResponseEntity<UserStats> getUserStats(
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "timeInterval", required = false) TimeInterval timeInterval) {

        LOGGER.debug("Received request get user statistics from: {}", fromDate);

        return ResponseEntity.status(HttpStatus.OK).body(personManager.getUserStats(fromDate, timeInterval));
    }
    
}
