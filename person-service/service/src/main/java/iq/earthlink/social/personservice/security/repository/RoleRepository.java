package iq.earthlink.social.personservice.security.repository;

import iq.earthlink.social.personservice.security.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Set<Role> findAllByCodeIn(List<String> codes);
}
