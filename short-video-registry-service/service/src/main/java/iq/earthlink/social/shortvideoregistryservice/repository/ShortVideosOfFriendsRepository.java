package iq.earthlink.social.shortvideoregistryservice.repository;

import iq.earthlink.social.shortvideoregistryservice.model.ShortVideosOfFriends;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Slice;

import java.sql.Timestamp;
import java.util.UUID;

public interface ShortVideosOfFriendsRepository extends CassandraRepository<ShortVideosOfFriends, UUID> {

    Slice<ShortVideosOfFriends> findByUserIdAndYearAndAuthorUserNameAndCreatedAtGreaterThanEqual(Long userId, int year,
                                                                                                 String friendUserName,
                                                                                                 Timestamp timestamp,
                                                                                                 CassandraPageRequest pageable);
}
