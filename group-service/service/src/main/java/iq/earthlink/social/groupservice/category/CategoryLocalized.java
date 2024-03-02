package iq.earthlink.social.groupservice.category;

import lombok.*;

import javax.persistence.*;

@Entity
@Data
@Builder
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "locale"})})
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "category")
@ToString(exclude = "category")
public class CategoryLocalized {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_localized_seq_gen")
    @SequenceGenerator(name = "category_localized_seq_gen", sequenceName = "category_localized_seq_gen", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private String locale;

    private String name;
}
