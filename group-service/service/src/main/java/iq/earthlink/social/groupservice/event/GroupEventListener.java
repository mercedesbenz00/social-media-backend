package iq.earthlink.social.groupservice.event;

import com.rabbitmq.client.Channel;
import iq.earthlink.social.classes.enumeration.GroupEventType;
import iq.earthlink.social.common.data.event.GroupActivityEvent;
import iq.earthlink.social.common.util.RabbitMQUtil;
import iq.earthlink.social.groupservice.group.GroupStatistics;
import iq.earthlink.social.groupservice.group.GroupStatisticsRepository;
import lombok.NonNull;
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

import static iq.earthlink.social.common.data.event.GroupActivityEvent.GROUP_ACTIVITY_EXCHANGE;
import static iq.earthlink.social.common.data.event.GroupActivityEvent.GROUP_ACTIVITY_QUEUE;

@Component
@Slf4j
public class GroupEventListener {

    private final GroupStatisticsRepository groupRepository;

    public GroupEventListener(GroupStatisticsRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupEventListener.class);

    private final ConcurrentMap<Long, GroupStatistics> statistics = new ConcurrentHashMap<>();

    @RabbitListener(bindings =
    @QueueBinding(
            value = @Queue(
                    value = GROUP_ACTIVITY_QUEUE,
                    arguments = {@Argument(name = "x-dead-letter-exchange", value = "deadMessageExchange"),
                            @Argument(name = "x-dead-letter-routing-key", value = "deadMessage")}),
            exchange = @Exchange(value = GROUP_ACTIVITY_EXCHANGE, type = ExchangeTypes.FANOUT)
    ), ackMode = "MANUAL"
    )
    public void receivePostActivityEvents(@Payload GroupActivityEvent event, @Header(AmqpHeaders.CHANNEL) Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        LOGGER.debug("Received event: {} ", event.getGroupEventType());
        collectGroupStatistics(event, channel, tag);
    }

    private void collectGroupStatistics(GroupActivityEvent event, Channel channel, Long tag) {
        GroupEventType groupEventType = GroupEventType.valueOf(event.getGroupEventType());
        long delta = event.getDelta();
        GroupStatistics groupStats = initGroupStatistics(event, channel, tag);
        switch (groupEventType) {
            case MEMBER_JOINED:
                groupStats.setMembersDelta(groupStats.getMembersDelta() + delta);
                break;
            case MEMBER_LEFT:
                groupStats.setMembersDelta(groupStats.getMembersDelta() - delta);
                break;
            default:
                throw new IllegalArgumentException("This GroupEventType not supported here");
        }
        groupStats.setUserGroupId(event.getUserGroupId());
    }

    @Scheduled(cron = "${social.groupservice.statistics.cron}")
    @Transactional
    public void updateStatistics() {
        LOGGER.debug("Updating group statistics... Size = {}", statistics.size());
        AtomicReference<GroupStatistics> gs = new AtomicReference<>();
        try {
            statistics.keySet().forEach(id -> {
                gs.set(statistics.remove(id));
                groupRepository.updateGroupStatistics(gs.get());
                sendAcknowledgement(gs.get());
            });
        } catch (Exception ex) {
            sendNAcknowledgement(gs.get(), ex);
        } finally {
            LOGGER.debug("Finished updating group statistics.");
        }
    }

    @NonNull
    private GroupStatistics initGroupStatistics(GroupActivityEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        GroupStatistics gs = statistics.get(event.getUserGroupId());
        if (Objects.isNull(gs)) {
            gs = new GroupStatistics();
            statistics.put(event.getUserGroupId(), gs);
        }

        Map<Channel, Set<Long>> channelTagsMap = gs.getChannelTagsMap();
        if (channelTagsMap.isEmpty()) {
            channelTagsMap = new HashMap<>();
        }
        Set<Long> tags = channelTagsMap.get(channel);
        if (Objects.isNull(tags)) {
            tags = new HashSet<>();
        }
        tags.add(tag);
        channelTagsMap.put(channel, tags);
        gs.setChannelTagsMap(channelTagsMap);
        return gs;
    }

    private void sendAcknowledgement(GroupStatistics gs) {
        Map<Channel, Set<Long>> channelTagsMap = gs.getChannelTagsMap();
        channelTagsMap.forEach((key, tags) -> tags.forEach(tag -> RabbitMQUtil.basicAck(key, tag)));
    }

    private void sendNAcknowledgement(GroupStatistics gs, Exception ex) {
        if (Objects.nonNull(gs)) {
            Map<Channel, Set<Long>> channelTagsMap = gs.getChannelTagsMap();
            channelTagsMap.forEach((key, tags) -> tags.forEach(tag -> RabbitMQUtil.reject(key, tag, true)));
            LOGGER.error("Failed to update group statistics: {}. The error is: {}. ", gs, ex.getMessage());
        }
    }
}
