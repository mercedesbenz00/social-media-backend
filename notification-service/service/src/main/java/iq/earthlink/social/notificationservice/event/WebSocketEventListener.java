package iq.earthlink.social.notificationservice.event;

import iq.earthlink.social.notificationservice.model.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by rajeevkumarsingh on 25/07/17.
 */
@Component
@Slf4j
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketEventListener(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Received a new web socket connection");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        Object username = sessionAttributes != null ? sessionAttributes.get("username") : null;
        if (username != null) {
            log.info("User Disconnected : " + username);

            NotificationMessage<Serializable> chatMessage = NotificationMessage.builder()
                    .type(NotificationMessage.MessageType.LEAVE)
                    .sender(username.toString())
                    .build();

            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        }
    }
}
