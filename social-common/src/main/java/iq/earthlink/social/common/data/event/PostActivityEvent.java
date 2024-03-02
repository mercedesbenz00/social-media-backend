package iq.earthlink.social.common.data.event;

import lombok.Data;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.Serializable;
import java.util.Map;

@Data
@ToString
public class PostActivityEvent implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostActivityEvent.class);

    public static final String POST_ACTIVITY_EXCHANGE = "postActivityExchange";
    public static final String POST_ACTIVITY_QUEUE = "postActivity.queue";
    public static final String POST_EVENT_TYPE = "type";
    public static final String AUTHOR_ID = "authorId";
    public static final String AUTHOR_DISPLAY_NAME = "authorDisplayName";
    public static final String POST_ID = "postId";
    public static final String GROUP_ID = "groupId";
    public static final String USER_GROUP_TYPE = "accessType";

    public static void send(RabbitTemplate rabbitTemplate, Map<String, Object> data) {
        try {
            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend(POST_ACTIVITY_EXCHANGE, "", data);
            }
        } catch (Exception ex) {
            LOGGER.error("Couldn't send Post Activity Event to the rabbitmq", ex);
        }
    }
}


