package iq.earthlink.social.shortvideoregistryservice.repository;

import iq.earthlink.social.shortvideoregistryservice.model.ShortVideoByAuthor;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ShortVideosByAuthorRepository extends CassandraRepository<ShortVideoByAuthor, UUID> {


    Optional<Slice<ShortVideoByAuthor>> findByAuthorIdAndYearAndCreatedAtGreaterThanEqual(Long authorId, Integer year, LocalDate fromDate, Pageable pageable);

    Optional<ShortVideoByAuthor> findByAuthorIdAndYearAndCreatedAtAndId(long authorId, int year,  Timestamp createdAt, UUID id);
}
