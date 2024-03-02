package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.rest.PostStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "PostStatisticsApi", tags = "Post Statistics Api")
@RestController
@RequestMapping("/api/v1/stats")
public class PostStatisticsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostStatisticsApi.class);

    private final PostManager postManager;

    public PostStatisticsApi(PostManager postManager) {
        this.postManager = postManager;
    }

    @ApiOperation(
            value = "Returns post statistics for specified time period",
            response = PostStats.class)
    @ApiResponses({
            @ApiResponse(code = 500, message = "If any unexpected server error")
    })
    @GetMapping()
    public ResponseEntity<PostStats> getPostStats(
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "timeInterval", required = false) TimeInterval timeInterval) {
        LOGGER.debug("Received request get post statistics from: {}", fromDate);

        return ResponseEntity.status(HttpStatus.OK).body(postManager.getPostStats(fromDate, timeInterval));
    }
}
