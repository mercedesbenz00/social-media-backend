package iq.earthlink.social.shortvideoregistryservice.repository;

import iq.earthlink.social.shortvideoregistryservice.model.ShortVideoConfiguration;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShortVideoConfigurationRepository extends CassandraRepository<ShortVideoConfiguration, UUID> {

    //@Query(value = "select * from short_video_config where person_id=?0 allow filtering")
    @AllowFiltering
    Optional<ShortVideoConfiguration> findByPersonId(Long personId);
}
