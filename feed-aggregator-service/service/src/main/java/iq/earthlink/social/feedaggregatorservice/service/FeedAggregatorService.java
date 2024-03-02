package iq.earthlink.social.feedaggregatorservice.service;

import iq.earthlink.social.feedaggregatorservice.dto.PostDTO;

import java.util.List;

public interface FeedAggregatorService {

    List<PostDTO> getFeedByGroups(List<Long> groupIds);
}
