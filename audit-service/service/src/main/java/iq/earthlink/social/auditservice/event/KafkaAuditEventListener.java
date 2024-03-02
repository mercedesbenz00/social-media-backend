package iq.earthlink.social.auditservice.event;

import iq.earthlink.social.auditservice.dto.JsonAuditLog;
import iq.earthlink.social.auditservice.model.AuditLog;
import iq.earthlink.social.auditservice.service.AuditService;
import iq.earthlink.social.common.util.CommonConstants;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaAuditEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaAuditEventListener.class);

    private final AuditService auditService;

    @KafkaListener(topics = CommonConstants.AUDIT_LOGS, groupId = "person-service-audit-logs-processing",
            containerFactory = "auditLogListenerContainerFactory")
    public void processedAuditLogListener(@Payload JsonAuditLog result) {
        LOGGER.debug("inside processedAuditLogListener: log id = " + result.getId());
        try {
            if (auditService.getAuditLogById(result.getId()) == null) {
                auditService.createAuditLog(AuditLog.builder()
                        .id(result.getId())
                        .category(result.getCategory())
                        .action(result.getAction())
                        .message(result.getMessage())
                        .authorId(result.getAuthorId())
                        .referenceId(result.getReferenceId())
                        .referenceName(result.getReferenceName())
                        .eventDate(result.getEventDate())
                        .build());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }
}
