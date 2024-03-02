package iq.earthlink.social.postservice.group.model;

import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.postservice.group.AccessType;
import iq.earthlink.social.postservice.group.PostingPermission;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Objects;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Cacheable
@Table(uniqueConstraints={@UniqueConstraint(columnNames = {"name"})})
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_seq_gen")
    @SequenceGenerator(name = "group_seq_gen", sequenceName = "group_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long groupId; //id from the group service

    @NotNull
    @Length(max = 255)
    @Column(nullable = false)
    private String name;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private MediaFile avatar;

    @Enumerated(EnumType.STRING)
    private AccessType accessType = AccessType.PUBLIC;

    @Enumerated(EnumType.STRING)
    private PostingPermission postingPermission = PostingPermission.WITH_APPROVAL;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        UserGroup userGroup = (UserGroup) o;
        return id != null && Objects.equals(id, userGroup.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
