package iq.earthlink.social.feedaggregatorservice.service;

import iq.earthlink.social.feedaggregatorservice.dto.PostDTO;
import iq.earthlink.social.feedaggregatorservice.dto.RecentPost;
import iq.earthlink.social.feedaggregatorservice.util.SortedListsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "social.mode", havingValue = "recentMode")
public class RecentFeedAggregatorServiceImpl implements FeedAggregatorService {


    private final RedisTemplate<String, RecentPost> redisTemplate;

    @Autowired
    public RecentFeedAggregatorServiceImpl(RedisTemplate<String, RecentPost> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    @Override
    @Cacheable(value = "spring.cache.feed.recent", key = "#groupIds.hashCode()")
    public List<PostDTO> getFeedByGroups(List<Long> groupIds) {
        List<List<RecentPost>> sortedLists = new ArrayList<>();
        for (Long groupId : groupIds) {
            String listKey = "group:" + groupId + ":posts";
            List<RecentPost> recentPosts = redisTemplate.opsForList().range(listKey, 0, -1);
            if (recentPosts == null) {
                continue;
            }
            recentPosts.forEach(recentPost -> recentPost.setUserGroupId(groupId));
            sortedLists.add(recentPosts.stream().filter(recentPost -> recentPost.getPublishedAt() != null).toList());
        }

        return SortedListsUtil.mergeSortedLists(sortedLists);
    }
}
