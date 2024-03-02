package iq.earthlink.social.groupservice.event.outbox.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import iq.earthlink.social.groupservice.event.outbox.MessageStatus;
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

    private String topic;

    private String key;

    @Type(type = "jsonb")
    @Column(name = "payload", columnDefinition = "jsonb")
    private Object payload;

    @Column(name = "status", columnDefinition = "char(32) default false")
    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "sent_at")
    private Date sentAt;

    @Column(name = "attempts_number", columnDefinition = "integer default 0")
    private int attemptsNumber;

    @NotNull
    @CreatedDate
    private Date createdAt;
}
