package iq.earthlink.social.common.util;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;

public class RabbitMQUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQUtil.class);

    private RabbitMQUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static void basicAck(Channel channel, long tag) {
        try {
            channel.basicAck(tag, false);
        } catch (IOException e) {
            LOGGER.warn("Failed to send acknowledgement to Rabbit MQ: {}", e.getMessage());
        }
    }

    public static void basicNack(Channel channel, long tag) {
        basicNack(channel, tag, true);
    }

    public static void basicNack(Channel channel, long tag, boolean requeue) {
        try {
            channel.basicNack(tag, false, requeue);
        } catch (IOException e) {
            LOGGER.warn("Failed to send negative acknowledgement to Rabbit MQ: {}", e.getMessage());
        }
    }

    public static void reject(Channel channel, long tag, boolean requeue) {
        try {
            channel.basicReject(tag, requeue);
        } catch (IOException e) {
            LOGGER.warn("Failed to send negative acknowledgement to Rabbit MQ: {}", e.getMessage());
        }
    }

    public static void send(RabbitTemplate rabbitTemplate, String exchange,  String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception ex) {
            LOGGER.error("Couldn't send Rabbitmq event to '{}' exchange", exchange, ex);
        }
    }
}
