package iq.earthlink.social.groupservice.event;

import iq.earthlink.social.classes.enumeration.GroupEventType;
import iq.earthlink.social.classes.enumeration.PostEventType;
import iq.earthlink.social.common.data.event.GroupMemberActivityEvent;
import iq.earthlink.social.common.data.event.PostGroupCountEvent;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.groupservice.group.member.GroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static iq.earthlink.social.classes.enumeration.PostEventType.POST_COUNT_UPDATED;
import static iq.earthlink.social.common.data.event.GroupMemberActivityEvent.GROUP_MEMBER_ACTIVITY_EXCHANGE;
import static iq.earthlink.social.common.data.event.GroupMemberActivityEvent.GROUP_MEMBER_ACTIVITY_QUEUE;
import static iq.earthlink.social.common.data.event.PostGroupCountEvent.POST_GROUP_EXCHANGE;

@Component
@Slf4j
public class GroupMemberEventListener {

    private final GroupMemberRepository repository;

    public GroupMemberEventListener(GroupMemberRepository repository) {
        this.repository = repository;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupMemberEventListener.class);


    @Transactional
    @RabbitListener(bindings =
    @QueueBinding(
            value = @Queue(value = GROUP_MEMBER_ACTIVITY_QUEUE, durable = "false"),
            exchange = @Exchange(value = GROUP_MEMBER_ACTIVITY_EXCHANGE, type = ExchangeTypes.FANOUT)
    )
    )
    public void receiveGroupMemberEvents(@Payload GroupMemberActivityEvent event) {
        LOGGER.info("Received GroupMemberActivityEvent event");
        GroupEventType groupEventType = GroupEventType.valueOf(event.getGroupEventType());
        switch (groupEventType) {
            case MEMBER_VISITED:
                updateVisitedDate(event);
                break;
            case MEMBER_DISPLAY_NAME_UPDATED:
                updateMemberDisplayName(event);
                break;
            default:
                LOGGER.warn("Received not supported event '{}'", groupEventType);
        }
    }

    @RabbitListener(bindings =
    @QueueBinding(
            value = @Queue(durable = "false"),
            exchange = @Exchange(value = POST_GROUP_EXCHANGE, type = ExchangeTypes.FANOUT)
    ))
    public void receivePostGroupCountEvent(@Payload PostGroupCountEvent event) {
        LOGGER.info("Received event: {} ", event.getEventType());
        PostEventType eventType = PostEventType.valueOf(event.getEventType());
        if (POST_COUNT_UPDATED.equals(eventType)) {
            List<GroupMember> groupMembersToUpdate = new ArrayList<>();
            // Update published posts count in group_member table:
            event.getGroupMemberPosts().forEach(o -> {
                Optional<GroupMember> gm = repository.findByPersonIdAndGroupId(o.getAuthorId(), o.getUserGroupId());
                gm.ifPresent(groupMember -> {
                            groupMember.setPublishedPostsCount(o.getPostsCount());
                            groupMembersToUpdate.add(gm.get());
                        }
                );
            });
            repository.saveAll(groupMembersToUpdate);
        }
    }

    private void updateVisitedDate(GroupMemberActivityEvent event) {
        if (Objects.nonNull(event.getGroupMemberId())) {
            try {
                GroupMember member = repository.findById(event.getGroupMemberId()).orElseThrow(() -> new NotFoundException("error.not.found.groupMember", event.getGroupMemberId()));
                member.setVisitedAt(new Date());
                repository.saveAndFlush(member);
            } catch (Exception ex) {
                LOGGER.error("Couldn't update visited date for group member: {}. The error message is: {}", event.getGroupMemberId(), ex.getMessage());
            }
        }
    }

    private void updateMemberDisplayName(GroupMemberActivityEvent event) {
        if (Objects.nonNull(event.getDisplayName()) && Objects.nonNull(event.getPersonId())) {
            try {
                repository.updateDisplayNameByPersonId(event.getPersonId(), event.getDisplayName());
            } catch (Exception ex) {
                LOGGER.error("Couldn't update displayName for group member with personId: {}", event.getPersonId());
            }
        } else {
            LOGGER.error("GroupMemberEventListener: Couldn't get display name or person Id from the event");
        }
    }

}
