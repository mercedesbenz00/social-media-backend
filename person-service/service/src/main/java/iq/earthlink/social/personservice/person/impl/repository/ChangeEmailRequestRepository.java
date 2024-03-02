package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.classes.enumeration.RequestState;
import iq.earthlink.social.personservice.person.model.ChangeEmailRequest;
import iq.earthlink.social.personservice.person.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeEmailRequestRepository extends JpaRepository<ChangeEmailRequest, Long> {

    List<ChangeEmailRequest> findByPersonAndNewEmailAndTokenAndState(Person person, String email, String token, RequestState state);
}
