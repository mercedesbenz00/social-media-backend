package iq.earthlink.social.userfeedaggregatorservice.controller;

import iq.earthlink.social.userfeedaggregatorservice.dto.FeedRequest;
import iq.earthlink.social.userfeedaggregatorservice.dto.FeedResponse;
import iq.earthlink.social.userfeedaggregatorservice.service.UserFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.ServiceUnavailableException;

@RestController
@RequestMapping(value = "/api/v1/feed", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserFeedApi {

    private final UserFeedService feedService;

    @GetMapping
    FeedResponse getUserFeed(@RequestHeader("Authorization") String authorizationHeader, FeedRequest feedRequest) throws ServiceUnavailableException {
        return feedService.getUserFeed(authorizationHeader, feedRequest);
    }
}
