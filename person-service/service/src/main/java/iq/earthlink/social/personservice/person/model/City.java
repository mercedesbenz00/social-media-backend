package iq.earthlink.social.personservice.person.model;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(indexes = {
    @Index(name = "unique_city_lang_idx", columnList = "name", unique = true)
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "city_seq_gen")
    @SequenceGenerator(name = "city_seq_gen", sequenceName = "city_seq_gen", allocationSize = 1)
    private Long id;

    @NotEmpty
    private String name;

    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "locale")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Map<String, CityLocalized> localizations = new HashMap<>();
}
