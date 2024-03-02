package iq.earthlink.social.personservice.person.model;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Data
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "locale"})})
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "city")
@ToString(exclude = "city")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CityLocalized {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "city_localized_seq_gen")
    @SequenceGenerator(name = "city_localized_seq_gen", sequenceName = "city_localized_seq_gen", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

    private String locale;

    private String name;
}
