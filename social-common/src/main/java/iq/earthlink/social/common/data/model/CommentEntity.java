package iq.earthlink.social.common.data.model;

import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(of = {"id", "objectId", "authorId", "content", "createdAt", "modifiedAt"})
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(indexes = {
        @Index(name = "comment_entity_created_at_idx", columnList = "createdAt"),
        @Index(name = "comment_entity_reply_to_idx", columnList = "reply_to_id")
})
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comment_entity_seq_gen")
    @SequenceGenerator(name = "comment_entity_seq_gen", sequenceName = "comment_entity_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @Length(max = 2000)
    @Column(length = 2000)
    private String content;

    @NotNull
    private Long authorId;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date modifiedAt;

    private Date deletedAt;

    @NotNull
    @Column(columnDefinition = "bool default false")
    private boolean isDeleted;

    @NotNull
    private UUID objectId;

    @ManyToOne
    @ToString.Exclude
    private CommentEntity replyTo;

    @OneToMany(mappedBy = "replyTo", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @ToString.Exclude
    private List<CommentEntity> replies;

    @NotNull
    @Column(columnDefinition = "int8 not null default 0")
    private long replyCommentsCount;
}
