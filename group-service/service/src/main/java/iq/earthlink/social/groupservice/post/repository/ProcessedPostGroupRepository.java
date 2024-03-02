package iq.earthlink.social.groupservice.post.repository;

import iq.earthlink.social.groupservice.post.model.ProcessedPostGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedPostGroupRepository extends JpaRepository<ProcessedPostGroup, Long> {
    Optional<ProcessedPostGroup> findByPostIdAndGroupId(Long postId, Long groupId);
}
