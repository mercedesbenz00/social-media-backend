package iq.earthlink.social.postservice.post.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.classes.enumeration.PostType;
import iq.earthlink.social.postservice.group.AccessType;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Table(indexes = {@Index(name = "post_author_idx", columnList = "authorId")})
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@EntityListeners(AuditingEntityListener.class)
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Post implements Serializable {

    public static final int MAX_TITLE_LENGTH = 255;
    public static final String PUBLISHED_AT = "publishedAt";
    public static final String CREATED_AT = "createdAt";
    public static final String SCORE = "score";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_seq_gen")
    @SequenceGenerator(name = "post_seq_gen", sequenceName = "post_seq_gen", allocationSize = 1)
    private Long id;

    @Column(name = "post_uuid", length = 36, nullable = false, updatable = false, unique = true)
    private UUID postUuid;

    @NotNull
    @CreatedDate
    private Date createdAt;

    private Date publishedAt;

    @Length(max = 4000)
    @Column(length = 4000)
    private String content;

    @NotNull
    @Column(nullable = false)
    private Long authorId;

    @NotNull
    @Column(nullable = false)
    private UUID authorUuid;

    @Length(max = 255)
    private String authorDisplayName;

    /**
     * Can be null, if user created post in it owns namespace
     */
    private Long userGroupId;

    @ManyToOne(fetch=FetchType.LAZY)
    @NotFound(action= NotFoundAction.IGNORE)
    @JoinColumn(name="reposted_from_id")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Post repostedFrom;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PostType postType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AccessType userGroupType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "char(32) default 'DRAFT'")
    private PostState state;

    // This need to order posts for feed
    private Date stateChangedDate;

    @Column(columnDefinition = "bool default true")
    private boolean commentsAllowed;

    // TODO(aleksey): this flag should be moved to collection level
    @Column(columnDefinition = "bool default false")
    private boolean pinned;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_mentioned_persons", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "mentioned_person_id")
    private List<Long> mentionedPersonIds = new ArrayList<>();

    @Type(type = "jsonb")
    @Column(name = "linkMeta", columnDefinition = "jsonb")
    private Map<String, String> linkMeta;

    @PrePersist
    public void autofill() {
        this.setPostUuid(UUID.randomUUID());
    }
}



