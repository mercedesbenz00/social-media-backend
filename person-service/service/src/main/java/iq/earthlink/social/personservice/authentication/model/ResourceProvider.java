package iq.earthlink.social.personservice.authentication.model;

import iq.earthlink.social.personservice.authentication.enumeration.ProviderName;
import iq.earthlink.social.personservice.person.model.Person;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Objects;

@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(of = {"id", "providerId", "providerName"})
@EntityListeners(AuditingEntityListener.class)
@Table(indexes = {@Index(name = "auth_provider_idx", columnList = "providerId, providerName", unique = true)})
public class ResourceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auth_provider_seq_gen")
    @SequenceGenerator(name = "auth_provider_seq_gen", sequenceName = "auth_provider_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    private String providerId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ProviderName providerName;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "person_id")
    private Person person;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceProvider)) return false;
        ResourceProvider provider = (ResourceProvider) o;
        return Objects.equals(getProviderId(), provider.getProviderId()) && Objects.equals(getProviderName(), provider.getProviderName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProviderId().concat(getProviderName().name()));
    }
}
