package iq.earthlink.social.postservice.person.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import iq.earthlink.social.common.file.MediaFile;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;

@Entity
@Getter
@Setter
@ToString
@Builder
@Table(
        indexes = {
                @Index(name = "person_generated_displayName_idx", columnList = "displayName"),
        }
)
@EntityListeners(AuditingEntityListener.class)
@Cacheable
@NoArgsConstructor
@AllArgsConstructor
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Person implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_seq_gen")
    @SequenceGenerator(name = "person_seq_gen", sequenceName = "person_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long personId;

    @Column(name = "person_uuid", length = 36, nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @NotNull
    @Column(nullable = false)
    private String displayName;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Set<String> personRoles = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private MediaFile avatar;

    @Column(columnDefinition = "boolean default false")
    private boolean isVerifiedAccount;

    public boolean isAdmin() {
        return getPersonRoles().contains("ADMIN");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Person person = (Person) o;
        return uuid != null && Objects.equals(uuid, person.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

