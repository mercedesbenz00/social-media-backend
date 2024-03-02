package iq.earthlink.social.shortvideoregistryservice.repository;

import iq.earthlink.social.shortvideoregistryservice.model.ShortVideoAuthorFriends;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.Optional;

public interface ShortVideoAuthorFriendsRepository extends CassandraRepository<ShortVideoAuthorFriends, Long> {

    Optional<List<ShortVideoAuthorFriends>> findByAuthorId(Long authorId);
}