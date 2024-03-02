package iq.earthlink.social.personservice.security.repository;

import iq.earthlink.social.personservice.security.BlockedToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BlockedTokenRepository extends CrudRepository<BlockedToken, Long> {

  Optional<BlockedToken> findByPersonIdAndKey(Long personId, String key);

}
