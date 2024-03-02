package iq.earthlink.social.postservice.post.complaint.model;

import iq.earthlink.social.postservice.post.PostComplaintState;
import iq.earthlink.social.postservice.post.model.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"authorId", "post_id"}))
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PostComplaint implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_complaint_seq_gen")
    @SequenceGenerator(name = "post_complaint_seq_gen", sequenceName = "post_complaint_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @ManyToOne
    private Post post;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @NotNull
    @Column(nullable = false)
    private Long authorId;

    @OneToOne
    @JoinColumn(name = "reason_id")
    private Reason reason;

    @Length(max = 1000)
    @Column(length = 1000)
    private String reasonOther;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "char(20) default 'PENDING'", nullable = false)
    private PostComplaintState state;

    private Long resolverId;

    @Length(max = 1000)
    @Column(length = 1000)
    private String resolvingText;

    private Date resolvingDate;
}
