package iq.earthlink.social.groupservice.event;

import iq.earthlink.social.groupservice.category.CategoryManager;
import iq.earthlink.social.groupservice.group.GroupManager;
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

@Component
@Slf4j
public class EventListener {

    private final GroupManager groupManager;
    private final CategoryManager categoryManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(EventListener.class);

    public static final String PERSON_DELETE_EVENT = "personDeleteInfo";

    public EventListener(GroupManager groupManager, CategoryManager categoryManager) {
        this.groupManager = groupManager;
        this.categoryManager = categoryManager;
    }

    @RabbitListener(bindings =
    @QueueBinding(
            value = @Queue(durable = "false"),
            exchange = @Exchange(value = PERSON_DELETE_EVENT, type = ExchangeTypes.FANOUT)
    ))
    public void receivePostActivityEvents(@Payload Long personId) {
        LOGGER.debug("Received event: {} ", PERSON_DELETE_EVENT);
        groupManager.removeMemberFromGroups(personId);
        categoryManager.removePersonCategories(personId);
    }

}
