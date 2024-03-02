package iq.earthlink.social.common.data.event;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.Serializable;

@Data
@Builder
@ToString
public class GroupActivityEvent implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupActivityEvent.class);

    public static final String GROUP_ACTIVITY_EXCHANGE = "groupActivityExchange";
    public static final String GROUP_ACTIVITY_QUEUE = "groupActivity.queue";

    private long userGroupId;
    private String groupEventType;
    private long delta;

    public GroupActivityEvent(Long userGroupId, String groupEventType) {
       this(userGroupId, groupEventType, 1);
    }

    public GroupActivityEvent(Long userGroupId, String groupEventType, long delta) {
        this.userGroupId = userGroupId;
        this.groupEventType = groupEventType;
        this.delta = delta;
    }

    public static void send(RabbitTemplate rabbitTemplate, GroupActivityEvent event) {
        try {
            rabbitTemplate.convertAndSend(GROUP_ACTIVITY_EXCHANGE, "", event);
        } catch (Exception ex) {
            LOGGER.error("Couldn't send event " + event.getGroupEventType() + " to the rabbitmq", ex);
        }
    }
}


