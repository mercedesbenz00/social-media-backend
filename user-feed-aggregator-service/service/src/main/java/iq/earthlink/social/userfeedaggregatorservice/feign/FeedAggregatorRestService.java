package iq.earthlink.social.userfeedaggregatorservice.feign;

import io.micrometer.core.annotation.Timed;
import iq.earthlink.social.userfeedaggregatorservice.dto.FeedAggregatorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "feed-aggregator-rest-service", url = "${feed.aggregator.service.url}")
public interface FeedAggregatorRestService {

    @GetMapping(path = "/api/v1/feed")
    @Timed(value = "userFeed.get.user.posts", description = "Time taken to return the list of post with details")
    List<FeedAggregatorResponse> getPosts(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestParam(value = "groupIds") List<Long> groupIds);
}
