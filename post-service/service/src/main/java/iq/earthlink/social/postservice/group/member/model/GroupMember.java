package iq.earthlink.social.postservice.group.member.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import iq.earthlink.social.postservice.group.member.enumeration.Permission;
import iq.earthlink.social.postservice.group.model.UserGroup;
import iq.earthlink.social.postservice.person.model.Person;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "group_member_unique_idx", columnList = "group_id, person_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Cacheable
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_member_seq")
    @SequenceGenerator(name = "group_member_seq", sequenceName = "group_member_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    @NotNull
    @ToString.Exclude
    private Person person;

    @NotNull
    @JoinColumn(name = "group_id")
    @ManyToOne
    private UserGroup userGroup;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @Type(type = "jsonb")
    @Column(name = "permissions", columnDefinition = "jsonb")
    private List<Permission> permissions;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        GroupMember that = (GroupMember) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}