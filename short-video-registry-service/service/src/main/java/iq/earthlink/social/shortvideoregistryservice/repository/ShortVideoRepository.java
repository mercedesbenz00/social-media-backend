package iq.earthlink.social.shortvideoregistryservice.repository;

import iq.earthlink.social.shortvideoregistryservice.model.ShortVideo;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public interface ShortVideoRepository extends CassandraRepository<ShortVideo, UUID> {

    @AllowFiltering
    List<ShortVideo> findByYearAfter(int monthYear);
}
