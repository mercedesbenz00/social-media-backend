package iq.earthlink.social.auditservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonAuditLog {
    String id;
    String action;
    String message;
    Long authorId;
    String category;
    Long referenceId;
    String referenceName;
    Date eventDate;
}
