package iq.earthlink.social.shortvideoregistryservice.repository;

import iq.earthlink.social.shortvideoregistryservice.model.ShortVideoVote;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShortVideoVoteRepository extends CassandraRepository<ShortVideoVote, UUID> {
    Optional<ShortVideoVote> findByPersonIdAndId(Long userId, UUID videoId);
}
