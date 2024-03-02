package iq.earthlink.social.personservice.authentication.repository;

import iq.earthlink.social.personservice.authentication.enumeration.ProviderName;
import iq.earthlink.social.personservice.authentication.model.ResourceProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResourceProviderRepository extends JpaRepository<ResourceProvider, Long> {

    Optional<ResourceProvider> findByProviderIdAndProviderName(String providerId, ProviderName providerName);
}
