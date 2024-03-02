package iq.earthlink.social.postservice.post.complaint.model;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.Length;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(uniqueConstraints={@UniqueConstraint(columnNames={"name"})})
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Reason implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reason_seq_gen")
    @SequenceGenerator(name = "reason_seq_gen", sequenceName = "reason_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @Length(max = 255)
    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "reason", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "locale")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Map<String, ReasonLocalized> localizations = new HashMap<>();

    public String getName() {
        Locale locale = LocaleContextHolder.getLocale();
        if (Objects.isNull(localizations.get(locale.getLanguage()))) {
            return name;
        }
        return localizations.get(locale.getLanguage()).getName();
    }
}
