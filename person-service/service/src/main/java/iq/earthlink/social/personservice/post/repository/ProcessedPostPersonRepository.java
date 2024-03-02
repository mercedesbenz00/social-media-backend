package iq.earthlink.social.personservice.post.repository;

import iq.earthlink.social.personservice.post.model.ProcessedPostPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedPostPersonRepository extends JpaRepository<ProcessedPostPerson, Long> {
    Optional<ProcessedPostPerson> findByPostIdAndPersonId(Long postId, Long personId);
}
