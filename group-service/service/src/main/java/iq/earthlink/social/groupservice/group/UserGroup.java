package iq.earthlink.social.groupservice.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.groupservice.category.Category;
import iq.earthlink.social.groupservice.group.rest.enumeration.AccessType;
import iq.earthlink.social.groupservice.group.rest.enumeration.*;
import iq.earthlink.social.groupservice.tag.Tag;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * User group model class for JPA (DB).
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints={@UniqueConstraint(columnNames = {"name"})})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_group_seq_gen")
    @SequenceGenerator(name = "user_group_seq_gen", sequenceName = "user_group_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @Length(max = 255)
    @Column(nullable = false)
    private String name;

    @NotNull
    @Length(max = 2000)
    @Column(length = 2000)
    private String description;

    @Length(max = 4000)
    @Column(length = 4000)
    private String rules;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @ToString.Exclude
    private Set<Category> categories = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private AccessType accessType = AccessType.PUBLIC;

    @Enumerated(EnumType.STRING)
    private PostingPermission postingPermission = PostingPermission.WITH_APPROVAL;

    @Enumerated(EnumType.STRING)
    private InvitePermission invitePermission = InvitePermission.ADMIN;

    /**
     * The person id who created the group.
     */
    @NotNull
    private Long ownerId;

    /**
     * This field keeps group statistic such as posts counts, etc
     * The actual statistic can be updated by handling events issued by the post-service, etc
     */
    @JsonIgnore
    @OneToOne(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private UserGroupStats stats;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ApprovalState state;

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private MediaFile cover;

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private MediaFile avatar;

    /**
     * Indicates, who can see the group info - everyone or invited people.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    private GroupVisibility visibility;
}
