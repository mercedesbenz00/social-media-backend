package iq.earthlink.social.common.data.event;

import lombok.Data;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.Serializable;

@Data
@ToString
public class GroupMemberActivityEvent implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupMemberActivityEvent.class);

    public static final String GROUP_MEMBER_ACTIVITY_EXCHANGE = "groupMemberActivityExchange";
    public static final String GROUP_MEMBER_ACTIVITY_QUEUE = "groupMemberActivity.queue";

    private Long groupMemberId;
    private Long personId;
    private Long groupId;
    private String displayName;
    private String groupEventType;

    public GroupMemberActivityEvent(Long groupMemberId, String groupEventType) {
        this.groupMemberId = groupMemberId;
        this.groupEventType = groupEventType;
    }

    public GroupMemberActivityEvent(Long personId, Long groupId, String groupEventType) {
        this.personId = personId;
        this.groupId = groupId;
        this.groupEventType = groupEventType;
    }

    public GroupMemberActivityEvent(Long personId, String displayName, String groupEventType) {
        this.personId = personId;
        this.displayName = displayName;
        this.groupEventType = groupEventType;
    }

    public static void send(RabbitTemplate rabbitTemplate, GroupMemberActivityEvent event) {
        try {
            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend(GROUP_MEMBER_ACTIVITY_EXCHANGE, "", event);
            }
        } catch (Exception ex) {
            LOGGER.error("Couldn't send Group Member Activity Event to the rabbitmq", ex);
        }
    }
}


