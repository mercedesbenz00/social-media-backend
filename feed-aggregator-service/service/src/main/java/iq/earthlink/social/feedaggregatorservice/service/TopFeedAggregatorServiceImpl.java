package iq.earthlink.social.feedaggregatorservice.service;

import iq.earthlink.social.feedaggregatorservice.dto.PostDTO;
import iq.earthlink.social.feedaggregatorservice.util.SortedListsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "social.mode", havingValue = "topkMode")
@Slf4j
public class TopFeedAggregatorServiceImpl implements FeedAggregatorService {

    private final UnifiedJedis jedis;

    @Autowired
    public TopFeedAggregatorServiceImpl(UnifiedJedis jedis) {
        this.jedis = jedis;
    }

    @Override
    @Cacheable(value="spring.cache.feed.topk", key = "#groupIds.hashCode()")
    public List<PostDTO> getFeedByGroups(List<Long> groupIds) {
        List<List<PostDTO>> sortedLists = new ArrayList<>();
        for (Long groupId : groupIds) {
            String topkListName = groupId.toString() + "_group_topk";
            List<String> topPostUuids = new ArrayList<>();
            try {
                topPostUuids = jedis.topkList(topkListName);
            } catch (JedisDataException ex) {
                log.info("TopK: key does not exist - " + topkListName);
            }
            if (topPostUuids == null) {
                continue;
            }
            List<PostDTO> topPosts = topPostUuids.stream()
                    .map(topPostUuid -> new PostDTO(topPostUuid, groupId))
                    .toList();
            sortedLists.add(topPosts);
        }

        return SortedListsUtil.combineLists(sortedLists);
    }
}
