package iq.earthlink.social.notificationservice.data.model;

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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_seq_gen")
    @SequenceGenerator(name = "email_seq_gen", sequenceName = "email_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    private String recipientEmail;

    private String subject;

    @Type(type = "jsonb")
    @Column(name = "template_model", columnDefinition = "jsonb")
    private Map<String, Object> templateModel;

    @Column(columnDefinition = "boolean default false")
    private boolean isSent;

    private Date sentAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    private EmailType type;

    @Column(columnDefinition = "integer default 0")
    private int attemptsNumber;

    @NotNull
    @CreatedDate
    private Date createdAt;

    private Long eventId;
}
