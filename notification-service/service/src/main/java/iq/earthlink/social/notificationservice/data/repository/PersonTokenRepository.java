package iq.earthlink.social.notificationservice.data.repository;

import iq.earthlink.social.notificationservice.data.model.PersonToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Optional;

public interface PersonTokenRepository extends JpaRepository<PersonToken, String> {
    Page<PersonToken> findByPersonId(Long personId, Pageable page);

    Page<PersonToken> findByPersonIdAndDevice(Long personId, String device, Pageable page);

    Optional<PersonToken> findByPersonIdAndDevice(Long personId, String device);

    void deleteByPushTokenAndPersonId(String pushToken, Long personId);

    Optional<PersonToken> findByPushTokenAndPersonId(String pushToken, Long personId);

    Long deleteByUpdatedAtBefore(Date oldestValidTime);
}
