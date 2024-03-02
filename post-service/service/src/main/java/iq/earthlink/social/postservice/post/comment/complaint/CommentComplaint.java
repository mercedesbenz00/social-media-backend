package iq.earthlink.social.postservice.post.comment.complaint;

import iq.earthlink.social.classes.enumeration.CommentComplaintState;
import iq.earthlink.social.postservice.post.comment.Comment;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"authorId", "comment_id"}))
@EntityListeners(AuditingEntityListener.class)
public class CommentComplaint {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comment_complaint_seq_gen")
    @SequenceGenerator(name = "comment_complaint_seq_gen", sequenceName = "comment_complaint_seq_gen", allocationSize = 1)
    private Long id;

    @Column(name = "complaint_uuid", length = 36, nullable = false, updatable = false, unique = true)
    private UUID complaintUuid;

    @ManyToOne
    @NotNull
    private Comment comment;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @NotNull
    private Long authorId;

    @OneToOne
    @JoinColumn(name = "reason_id")
    private Reason reason;

    @Length(max = 1000)
    @Column(length =  1000)
    private String reasonOther;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "char(20) default 'PENDING'")
    private CommentComplaintState state;

    private Long resolverId;

    @Length(max = 1000)
    @Column(length = 1000)
    private String resolvingText;

    private Date resolvingDate;

    @PrePersist
    public void autofill() {
        this.setComplaintUuid(UUID.randomUUID());
    }
}
