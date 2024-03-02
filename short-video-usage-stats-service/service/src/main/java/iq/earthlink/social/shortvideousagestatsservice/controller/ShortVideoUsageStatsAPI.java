package iq.earthlink.social.shortvideousagestatsservice.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.shortvideousagestatsservice.service.UsageStatsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "Short Video Usage Stats API", value = "ShortVideoUsageStatsAPI")
@RestController
@RequestMapping(value = "/api/v1/short-video-usage-stats", produces = MediaType.APPLICATION_JSON_VALUE)
public class ShortVideoUsageStatsAPI {

    private final UsageStatsService usageStatsService;

    public ShortVideoUsageStatsAPI(UsageStatsService usageStatsService) {
        this.usageStatsService = usageStatsService;
    }

    @ApiOperation("Log new event produced by user against the short video")
    @PostMapping
    public void addEvent(@RequestBody String inputData) {
        usageStatsService.addEvent(inputData);
    }
}
