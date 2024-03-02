package iq.earthlink.social.userfeedaggregatorservice.service;

import iq.earthlink.social.classes.data.dto.PostResponse;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.postservice.rest.PostRestService;
import iq.earthlink.social.security.SecurityProvider;
import iq.earthlink.social.userfeedaggregatorservice.dto.FeedAggregatorResponse;
import iq.earthlink.social.userfeedaggregatorservice.dto.FeedRequest;
import iq.earthlink.social.userfeedaggregatorservice.dto.FeedResponse;
import iq.earthlink.social.userfeedaggregatorservice.feign.FeedAggregatorRestService;
import iq.earthlink.social.userfeedaggregatorservice.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.naming.ServiceUnavailableException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserFeedServiceImpl implements UserFeedService {

    private final FeedAggregatorRestService feedAggregatorRestService;
    private final UserGroupsService userGroupsService;
    private final Utils utils;
    private final PostRestService postRestService;
    private final UserFeedCache feedCache;
    private final SecurityProvider securityProvider;

    @Value("${social.config.thresholdMinutes}")
    private Integer thresholdMinutes;

    @Value("${social.mode}")
    private String appMode;


    @Override
    public FeedResponse getUserFeed(String authorizationHeader, FeedRequest request) throws ServiceUnavailableException {
        int pageNumber = request.getPage();

        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Long timestamp = feedCache.getTimestamp(appMode, personId);
        FeedResponse feedResponse;
        log.info("Requesting user feed for the user with id: {}", personId);
        if (Objects.isNull(timestamp)) {
            pageNumber = 0;
            long timestampMillis = utils.getTimestamp();
            String key = getCacheKey(personId, timestampMillis);
            saveToCache(timestampMillis, authorizationHeader, personId);
            feedResponse = getPaginatedFromCache(authorizationHeader, key, request.getGroupIds(), timestampMillis, pageNumber, request.getSize());
        } else {
            String key = getCacheKey(personId, timestamp);
            if (Objects.isNull(request.getTimestamp()) || !Objects.equals(request.getTimestamp(), timestamp)) {
                pageNumber = 0;
            }

            feedResponse = getPaginatedFromCache(authorizationHeader, key, request.getGroupIds(), timestamp, pageNumber, request.getSize());

            if (utils.isTimestampExpired(timestamp, thresholdMinutes) || (!feedCache.isKeyExist(key)) || (pageNumber == 0 && CollectionUtils.isEmpty(feedResponse.getContent()))) {
                CompletableFuture.runAsync(() -> {
                    log.info("Feed cache expired or empty, trying to refresh feed for the user with id: {}", personId);
                    saveToCache(utils.getTimestamp(), authorizationHeader, personId);
                });
            }
        }
        return feedResponse;
    }

    @Async
    public void saveToCache(Long timestamp, String authorizationHeader, Long personId) {
        log.info("Updating user feed cache for the user with id: {}", personId);
        if (Boolean.FALSE.equals(feedCache.isFeedCacheLocked(appMode + "_" + personId))) {
            try {
                feedCache.lockFeedCache(appMode + "_" + personId);
                String key = getCacheKey(personId, timestamp);
                Set<Long> groupIds = userGroupsService.getUserGroupIdList(authorizationHeader);
                List<FeedAggregatorResponse> feed = feedAggregatorRestService.getPosts(authorizationHeader, groupIds.stream().toList());
                feedCache.invalidateUserFeed(appMode + "_" + personId);
                feedCache.saveUserFeed(key, feed);
                feedCache.setTimestamp(appMode, personId, timestamp);
                log.info("Updated user feed cache for the user with id: {}", personId);
            } catch (Exception ex) {
                log.error("Feed caching failed for the person with id: {} with error: {}", personId, ex);
            } finally {
                feedCache.unlockFeedCache(appMode + "_" + personId);
            }
        }
    }

    public FeedResponse getPaginatedFromCache(String authorizationHeader, String key, List<Long> groupIds, long timestamp, int pageNumber, int pageSize) throws ServiceUnavailableException {
        log.info("Fetching user feed from cache with key {} and page {}", key, pageNumber);
        int startIndex = pageNumber * pageSize;
        int endIndex = startIndex + pageSize - 1;
        Set<FeedAggregatorResponse> feed;
        List<PostResponse> posts;
        long totalElements = 0;
        try {
            if (CollectionUtils.isEmpty(groupIds)) {
                feed = feedCache.getUserFeedInRange(key, startIndex, endIndex);
                totalElements = feedCache.getUserFeedSize(key);
            } else {
                String keyWithGroupIds = getCacheKeyWithGroups(key, groupIds);
                if (Boolean.FALSE.equals(feedCache.isKeyExist(keyWithGroupIds))) {
                    var userFeed = feedCache.getUserFeed(key);
                    var filteredUserFeed = userFeed.stream().filter(feedItem -> groupIds.contains(feedItem.getUserGroupId())).toList();
                    feedCache.saveUserFeed(keyWithGroupIds, filteredUserFeed);
                }
                feed = feedCache.getUserFeedInRange(keyWithGroupIds, startIndex, endIndex);
                totalElements = feedCache.getUserFeedSize(keyWithGroupIds);
            }
            posts = postRestService.getPosts(authorizationHeader, feed.stream().map(FeedAggregatorResponse::getPostUuid).toList());
        } catch (Exception ex) {
            feedCache.invalidateUserFeed(key);
            throw new RestApiException(HttpStatus.SERVICE_UNAVAILABLE, "Exception while constructing the feed:" + ex.getMessage());
        }
        long totalPages = totalElements / pageSize;

        if (totalElements % pageSize != 0) {
            totalPages++;
        }

        return FeedResponse
                .builder()
                .content(posts)
                .number(pageNumber)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .timestamp(timestamp)
                .size(pageSize)
                .build();
    }

    public String getCacheKey(Long personId, Long timestamp) {
        return appMode + "_" + personId + "_" + timestamp;
    }

    public String getCacheKeyWithGroups(String key, List<Long> groupIds) {
        Collections.sort(groupIds);

        String groupIdsString = groupIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        return key + "_" + utils.calculateHash(groupIdsString);
    }
}



