package iq.earthlink.social.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iq.earthlink.social.exception.RestApiException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static iq.earthlink.social.common.util.CommonConstants.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class AuditMessage {
    String id;
    Date eventDate;
    String message;
    EventAction action;
    Long authorId;
    Long referenceId;

    public static String create(EventAction action, String logMessage, Long authorId, Long referenceId) {
        UUID uuid = UUID.randomUUID();

        return AuditMessage.builder()
                .id(uuid.toString())
                .message(logMessage)
                .action(action)
                .authorId(authorId)
                .referenceId(referenceId)
                .eventDate(new Date())
                .build()
                .toJSON();
    }

    public String toJSON() {
        Map<String, Object> map = new HashMap<>();
        map.put(ID, id);
        map.put(MESSAGE, message);
        map.put(AUTHOR_ID, authorId);
        map.put(REFERENCE_ID, referenceId);
        map.put(REFERENCE_NAME, action.getReferenceName());
        map.put(EVENT_DATE, eventDate);
        map.put(CATEGORY, action.getCategory());
        map.put(ACTION, action.name());
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RestApiException(HttpStatus.EXPECTATION_FAILED, e.getMessage(), e);
        }

    }
}
