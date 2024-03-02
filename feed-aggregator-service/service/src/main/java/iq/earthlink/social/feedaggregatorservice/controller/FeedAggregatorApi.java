package iq.earthlink.social.feedaggregatorservice.controller;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import iq.earthlink.social.feedaggregatorservice.dto.PostDTO;
import iq.earthlink.social.feedaggregatorservice.service.FeedAggregatorService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/feed", produces = MediaType.APPLICATION_JSON_VALUE)
public class FeedAggregatorApi {

    private final FeedAggregatorService feedAggregatorService;
    private final MeterRegistry meterRegistry;

    public FeedAggregatorApi(FeedAggregatorService feedAggregatorService, MeterRegistry meterRegistry) {
        this.feedAggregatorService = feedAggregatorService;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping
    @Timed(value = "feedAggregator.getFeedByGroupIds.time")
    public List<PostDTO> getFeedByGroupIds(
            @RequestParam List<Long> groupIds) {
        Collections.sort(groupIds);

        Gauge.builder("feedAggregator.groupIds.count", groupIds, List::size)
                .description("Number of groupIds in the request")
                .register(meterRegistry);

        List<PostDTO> feedByGroups = feedAggregatorService.getFeedByGroups(groupIds);

        Gauge.builder("feedAggregator.feedByGroups.count", feedByGroups, List::size)
                .description("Number of posts in the response")
                .register(meterRegistry);

        return feedByGroups;
    }
}
