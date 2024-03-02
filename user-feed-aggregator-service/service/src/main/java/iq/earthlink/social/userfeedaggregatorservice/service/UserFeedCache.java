package iq.earthlink.social.userfeedaggregatorservice.service;

import iq.earthlink.social.userfeedaggregatorservice.dto.FeedAggregatorResponse;

import java.util.List;
import java.util.Set;

public interface UserFeedCache {

    Long getTimestamp(String mode, Long personId);
    void setTimestamp(String mode, Long personId, long timestamp);
    void saveUserFeed(String key, List<FeedAggregatorResponse> feed);
    Set<FeedAggregatorResponse> getUserFeedInRange(String key, int startIndex, int endIndex);
    boolean isKeyExist(String key);
    Set<FeedAggregatorResponse> getUserFeed(String key);
    void invalidateUserFeed(String key);
    void lockFeedCache(String lockKey);
    void unlockFeedCache(String lockKey);
    Boolean isFeedCacheLocked(String lockKey);
    long getUserFeedSize(String key);

}
