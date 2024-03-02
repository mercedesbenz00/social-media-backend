package iq.earthlink.social.personservice.outbox.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import iq.earthlink.social.classes.enumeration.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "outbox_messages")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "email_type")
    private EmailType emailType;

    @Type(type = "jsonb")
    @Column(name = "template_model", columnDefinition = "jsonb")
    private Map<String, Object> templateModel;

    @Column(name = "is_sent", columnDefinition = "boolean default false")
    private boolean isSent;

    @Column(name = "sent_at")
    private Date sentAt;

    @Column(name = "attempts_number", columnDefinition = "integer default 0")
    private int attemptsNumber;

    @NotNull
    @CreatedDate
    private Date createdAt;
}
