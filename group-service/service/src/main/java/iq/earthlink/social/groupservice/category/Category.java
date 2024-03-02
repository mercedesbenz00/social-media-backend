package iq.earthlink.social.groupservice.category;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iq.earthlink.social.common.file.MediaFile;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.Length;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@EntityListeners(AuditingEntityListener.class)
@Table(uniqueConstraints={@UniqueConstraint(columnNames={"name"})})
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_seq_gen")
    @SequenceGenerator(name = "category_seq_gen", sequenceName = "category_seq_gen", allocationSize = 1)
    private Long id;


    @Column(name = "category_uuid", length = 36, nullable = false, updatable = false, unique = true)
    private UUID categoryUUID;

    @NotNull
    @Length(max = 255)
    @Column(nullable = false)
    private String name;

    @JsonIgnore
    @Column(columnDefinition = "bool default false", nullable = false)
    private boolean deleted;

    @ManyToOne
    private Category parentCategory;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @OneToMany(mappedBy = "parentCategory", orphanRemoval = true, cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private List<Category> children;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "locale")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Map<String, CategoryLocalized> localizations = new HashMap<>();

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private MediaFile cover;

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private MediaFile avatar;

    @Column(columnDefinition = "int8 not null default 0")
    private long personCount;

    @Column(columnDefinition = "int8 not null default 0")
    private long groupCount;

    public String getName() {
        Locale locale = LocaleContextHolder.getLocale();
        if (Objects.isNull(localizations.get(locale.getLanguage()))) {
            return name;
        }
        return localizations.get(locale.getLanguage()).getName();
    }

    @PrePersist
    public void autofill() {
        this.setCategoryUUID(UUID.randomUUID());
    }

    public boolean isRootCategory() {
        return this.parentCategory == null;
    }
}
