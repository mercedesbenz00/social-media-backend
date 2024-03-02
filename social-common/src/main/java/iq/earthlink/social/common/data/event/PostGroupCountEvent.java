package iq.earthlink.social.common.data.event;

import iq.earthlink.social.classes.data.dto.GroupMemberPosts;
import lombok.Data;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.Serializable;
import java.util.List;

@Data
@ToString
public class PostGroupCountEvent implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostGroupCountEvent.class);

    public static final String POST_GROUP_EXCHANGE = "postGroupExchange";

    private String eventType;
    private List<GroupMemberPosts> groupMemberPosts;

    public PostGroupCountEvent(String postEventType, List<GroupMemberPosts> postCounts) {
        this.eventType = postEventType;
        this.groupMemberPosts = postCounts;
    }

    public static void send(RabbitTemplate rabbitTemplate, PostGroupCountEvent event) {
        try {
            rabbitTemplate.convertAndSend(POST_GROUP_EXCHANGE, "", event);
        } catch (Exception ex) {
            LOGGER.error("Couldn't send event " + event.getEventType() + " to the rabbitmq", ex);
        }
    }
}


