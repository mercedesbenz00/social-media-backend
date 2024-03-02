package iq.earthlink.social.personservice.security.repository;

import iq.earthlink.social.personservice.security.model.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {

  Set<Authority> findAllByCodeIn(List<String> codes);
}
