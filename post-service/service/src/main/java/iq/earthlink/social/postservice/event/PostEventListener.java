package iq.earthlink.social.postservice.event;

import com.rabbitmq.client.Channel;
import iq.earthlink.social.classes.enumeration.PostEventType;
import iq.earthlink.social.common.data.event.PostActivityEvent;
import iq.earthlink.social.common.util.RabbitMQUtil;
import iq.earthlink.social.postservice.group.AccessType;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.PostStatisticsDTO;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import iq.earthlink.social.postservice.post.statistics.PostStatisticsRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import static iq.earthlink.social.classes.enumeration.PostEventType.*;
import static iq.earthlink.social.common.data.event.PostActivityEvent.POST_ACTIVITY_EXCHANGE;
import static iq.earthlink.social.common.data.event.PostActivityEvent.POST_ACTIVITY_QUEUE;

@Component
@Slf4j
@RequiredArgsConstructor
public class PostEventListener {

    private final PostStatisticsRepository postStatisticsRepository;
    private final PostRepository postRepository;
    private final PostManager postManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(PostEventListener.class);

    private final ConcurrentMap<Long, PostStatisticsDTO> statistics = new ConcurrentHashMap<>();

    @RabbitListener(bindings =
    @QueueBinding(
            value = @Queue(
                    value = POST_ACTIVITY_QUEUE,
                    arguments = {@Argument(name = "x-dead-letter-exchange", value = "deadMessageExchange"),
                            @Argument(name = "x-dead-letter-routing-key", value = "deadMessage")}),
            exchange = @Exchange(value = POST_ACTIVITY_EXCHANGE, type = ExchangeTypes.FANOUT)
    ), ackMode = "MANUAL"
    )
    public void receivePostActivityEvents(@Payload Map<String, Object> eventData, @Header(AmqpHeaders.CHANNEL) Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        LOGGER.debug("Received event: {} ", eventData.get(PostActivityEvent.POST_EVENT_TYPE));

        PostEventType postEventType = PostEventType.valueOf(eventData.get(PostActivityEvent.POST_EVENT_TYPE).toString());
        if (PURGE_STATS.equals(postEventType)) {
            statistics.clear();
            RabbitMQUtil.basicAck(channel, tag);
        } else {
            Long postId = Long.valueOf(eventData.get(PostActivityEvent.POST_ID).toString());
            collectPostStatistics(postEventType, postId, channel, tag);
        }
    }


    @RabbitListener(bindings =
    @QueueBinding(
            value = @Queue(value = "tempQueue", durable = "false", autoDelete = "true"),
            exchange = @Exchange(value = "tempExchange", type = ExchangeTypes.FANOUT, durable = "false", autoDelete = "true")
    ))
    public void receiveGroupAccessTypes(@Payload Map<Long, AccessType> groupAccessTypes) {
        LOGGER.debug("Received group access types: {} ", groupAccessTypes);
        groupAccessTypes.forEach(postManager::updatePostGroupType);
    }

    private void collectPostStatistics(PostEventType type, Long postId, Channel channel, long tag) {

        PostStatisticsDTO ps = initPostStatistics(postId, channel, tag);

        switch (type) {
            case POST_COMMENT_ADDED:
                ps.setCommentsDelta(ps.getCommentsDelta() + 1);
                break;
            case POST_COMMENT_REMOVED:
                ps.setCommentsDelta(ps.getCommentsDelta() - 1);
                break;
            case POST_UPVOTE_ADDED:
                ps.setUpvotesDelta(ps.getUpvotesDelta() + 1);
                break;
            case POST_DOWNVOTE_ADDED:
                ps.setDownvotesDelta(ps.getDownvotesDelta() + 1);
                break;
            case POST_UPVOTE_REMOVED:
                ps.setUpvotesDelta(ps.getUpvotesDelta() - 1);
                break;
            case POST_DOWNVOTE_REMOVED:
                ps.setDownvotesDelta(ps.getDownvotesDelta() - 1);
                break;
            case POST_COMMENT_UPVOTE_ADDED:
                ps.setCommentsUpvotesDelta(ps.getCommentsUpvotesDelta() + 1);
                break;
            case POST_COMMENT_DOWNVOTE_ADDED:
                ps.setCommentsDownvotesDelta(ps.getCommentsDownvotesDelta() + 1);
                break;
            case POST_COMMENT_UPVOTE_REMOVED:
                ps.setCommentsUpvotesDelta(ps.getCommentsUpvotesDelta() - 1);
                break;
            case POST_COMMENT_DOWNVOTE_REMOVED:
                ps.setCommentsDownvotesDelta(ps.getCommentsDownvotesDelta() - 1);
                break;
            default:
                throw new IllegalArgumentException("This PostEventType is not supported here");
        }
        ps.setPostId(postId);
        ps.setLastActivityAt(new Date(System.currentTimeMillis()));
    }

    @Scheduled(cron = "${social.postservice.statistics.cron}")
    @Transactional
    public void updateStatistics() {
        LOGGER.debug("Updating post statistics... Size = {}", statistics.size());
        AtomicReference<PostStatisticsDTO> ps = new AtomicReference<>();
        try {
            if (!statistics.isEmpty()) {
                statistics.keySet().forEach(id -> {
                    ps.set(statistics.remove(id));
                    PostStatisticsDTO postStatisticsDTO = ps.get();
                    if (postRepository.findById(postStatisticsDTO.getPostId()).isPresent()) {
                        postStatisticsRepository.updatePostStatistics(postStatisticsDTO);
                        sendAcknowledgement(ps.get());
                    }
                });
            }
        } catch (Exception ex) {
            sendNAcknowledgement(ps.get(), ex);
        } finally {
            LOGGER.debug("Finished updating post statistics.");
        }
    }

    @Scheduled(cron = "${social.postservice.statistics.sync}")
    @Transactional
    public void syncPostStatistics() {
        LOGGER.debug("Synchronizing post statistics...");
        try {
            statistics.clear();
            postStatisticsRepository.synchronizePostStatistics();
            postStatisticsRepository.updatePostScore();
            LOGGER.debug("Finished updating post statistics.");
        } catch (Exception ex) {
            LOGGER.error("Error while synchronizing post statistics.");
        }
    }

    @NonNull
    private PostStatisticsDTO initPostStatistics(Long postId, Channel channel, long tag) {
        PostStatisticsDTO ps = statistics.get(postId);
        if (Objects.isNull(ps)) {
            ps = new PostStatisticsDTO();
            statistics.put(postId, ps);
        }
        Map<Channel, Set<Long>> channelTagsMap = ps.getChannelTagsMap();
        if (channelTagsMap.isEmpty()) {
            channelTagsMap = new HashMap<>();
        }
        Set<Long> tags = channelTagsMap.get(channel);
        if (Objects.isNull(tags)) {
            tags = new HashSet<>();
        }
        tags.add(tag);
        channelTagsMap.put(channel, tags);
        ps.setChannelTagsMap(channelTagsMap);
        return ps;
    }

    private void sendAcknowledgement(PostStatisticsDTO ps) {
        Map<Channel, Set<Long>> channelTagsMap = ps.getChannelTagsMap();
        channelTagsMap.forEach((key, tags) -> tags.forEach(tag -> RabbitMQUtil.basicAck(key, tag)));
    }

    private void sendNAcknowledgement(PostStatisticsDTO ps, Exception ex) {
        if (Objects.nonNull(ps)) {
            Map<Channel, Set<Long>> channelTagsMap = ps.getChannelTagsMap();
            channelTagsMap.forEach((key, tags) -> tags.forEach(tag -> RabbitMQUtil.reject(key, tag, true)));
            LOGGER.error("Failed to update post statistics: {}. The error is: {}. ", ps, ex.getMessage());
        }
    }
}
