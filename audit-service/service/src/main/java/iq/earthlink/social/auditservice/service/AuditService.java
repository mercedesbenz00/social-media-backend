package iq.earthlink.social.auditservice.service;

import iq.earthlink.social.auditservice.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuditService {

    AuditLog createAuditLog(AuditLog log);
    List<AuditLog> findLogsBySearchCriteria(AuditLogSearchCriteria criteria);
    Page<AuditLog> findLogsBySearchCriteria(AuditLogSearchCriteria criteria, Pageable page);
    AuditLog getAuditLogById(String id);
}
