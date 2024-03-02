package iq.earthlink.social.auditservice.service;

import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.common.audit.EventCategory;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogSearchCriteria {
    private String id;
    private Long authorId;
    private String query;
    private EventCategory category;
    private EventAction action;
}

