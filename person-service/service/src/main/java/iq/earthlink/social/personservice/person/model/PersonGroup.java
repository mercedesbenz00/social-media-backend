package iq.earthlink.social.personservice.person.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_person_group_person_id_group_id", columnNames = {"personId", "groupId"})
})
public class PersonGroup {
    @Id
    @ApiModelProperty("Id of person groups")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_group_seq_gen")
    @SequenceGenerator(name = "person_group_seq_gen", sequenceName = "person_group_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    private Long personId;

    @NotNull
    private Long groupId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        PersonGroup that = (PersonGroup) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
