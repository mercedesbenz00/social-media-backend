package iq.earthlink.social.postservice.post.comment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iq.earthlink.social.postservice.post.model.Post;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(of = {"id", "post", "authorId", "content", "createdAt", "modifiedAt"})
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(indexes = {
    @Index(name = "comment_created_at_idx", columnList = "createdAt"),
    @Index(name = "comment_reply_to_idx", columnList = "reply_to_id")
})
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Comment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comment_seq_gen")
    @SequenceGenerator(name = "comment_seq_gen", sequenceName = "comment_seq_gen", allocationSize = 1)
    private Long id;

    @Column(name = "comment_uuid", length = 36, nullable = false, updatable = false, unique = true)
    private UUID commentUuid;

    @Length(max = 2000)
    @Column(length = 2000)
    private String content;

    @NotNull
    private Long authorId;

    @NotNull
    @Column(nullable = false)
    private UUID authorUuid;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date modifiedAt;

    private Long modifiedBy;

    private Date deletedAt;

    @NotNull
    @Column(columnDefinition = "bool default false")
    private boolean isDeleted;

    @NotNull
    @ManyToOne
    @JsonIgnore
    @ToString.Exclude
    private Post post;

    @ManyToOne
    @ToString.Exclude
    private Comment replyTo;

    @OneToMany(mappedBy = "replyTo", cascade = CascadeType.REMOVE, fetch=FetchType.EAGER, orphanRemoval = true)
    @Fetch(value = FetchMode.SUBSELECT)
    @ToString.Exclude
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Comment> replies;

    @NotNull
    @Column(columnDefinition = "int8 not null default 0")
    private long replyCommentsCount;

    @PrePersist
    public void autofill() {
        this.setCommentUuid(UUID.randomUUID());
    }
}
