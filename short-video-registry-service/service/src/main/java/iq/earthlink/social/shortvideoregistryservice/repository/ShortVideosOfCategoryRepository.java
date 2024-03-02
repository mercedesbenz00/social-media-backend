package iq.earthlink.social.shortvideoregistryservice.repository;

import iq.earthlink.social.shortvideoregistryservice.model.ShortVideosOfCategory;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Slice;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ShortVideosOfCategoryRepository extends CassandraRepository<ShortVideosOfCategory, UUID> {

    Optional<Slice<ShortVideosOfCategory>> findByCategoryIdInAndYearAndCreatedAtGreaterThanEqual(Collection<UUID> categoryId,
                                                                                                 int year,
                                                                                                 Timestamp createdAt,
                                                                                                 CassandraPageRequest pageable);

}
