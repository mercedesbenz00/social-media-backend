package iq.earthlink.social.userfeedaggregatorservice.service;

import iq.earthlink.social.userfeedaggregatorservice.dto.FeedRequest;
import iq.earthlink.social.userfeedaggregatorservice.dto.FeedResponse;

import javax.naming.ServiceUnavailableException;

public interface UserFeedService {

    FeedResponse getUserFeed(String authorizationHeader, FeedRequest request) throws ServiceUnavailableException;
}
