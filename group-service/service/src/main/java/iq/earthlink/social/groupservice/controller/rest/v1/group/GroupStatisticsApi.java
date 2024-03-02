package iq.earthlink.social.groupservice.controller.rest.v1.group;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.groupservice.category.CategoryManager;
import iq.earthlink.social.groupservice.category.dto.CategoryStats;
import iq.earthlink.social.groupservice.group.GroupManager;
import iq.earthlink.social.groupservice.group.dto.GroupStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "GroupStatisticsApi", tags = "Group Statistics Api")
@RestController
@RequestMapping(value = "/api/v1/stats", produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupStatisticsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupStatisticsApi.class);

    private final GroupManager groupManager;
    private final CategoryManager categoryManager;

    public GroupStatisticsApi(GroupManager groupManager, CategoryManager categoryManager) {
        this.groupManager = groupManager;
        this.categoryManager = categoryManager;
    }

    @ApiOperation(
            value = "Returns group statistics for specified time period",
            response = GroupStats.class)
    @ApiResponses({
            @ApiResponse(code = 500, message = "If any unexpected server error")
    })
    @GetMapping()
    public ResponseEntity<GroupStats> getGroupStats(
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "timeInterval", required = false) TimeInterval timeInterval) {
        LOGGER.debug("Received request get group statistics from: {}", fromDate);

        return ResponseEntity.status(HttpStatus.OK).body(groupManager.getGroupStats(fromDate, timeInterval));
    }

    @ApiOperation(
            value = "Returns group category statistics for specified time period",
            response = CategoryStats.class)
    @ApiResponses({
            @ApiResponse(code = 500, message = "If any unexpected server error")
    })
    @GetMapping("/category")
    public ResponseEntity<CategoryStats> getGroupCategoryStats(
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "timeInterval", required = false) TimeInterval timeInterval) {
        LOGGER.debug("Received request get group statistics from: {}", fromDate);

        return ResponseEntity.status(HttpStatus.OK).body(categoryManager.getCategoryStats(fromDate, timeInterval));
    }
}
