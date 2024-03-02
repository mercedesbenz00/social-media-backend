package iq.earthlink.social.shortvideoregistryservice.repository;

import iq.earthlink.social.shortvideoregistryservice.model.Category;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CategoryRepository extends CassandraRepository<Category, UUID> {
    @AllowFiltering
    Optional<Category> findByName(String name);

    @AllowFiltering
    Optional<List<Category>> findByNameIn(List<String> names);

    @AllowFiltering
    Optional<List<Category>> findByIdIn(Set<UUID> categories);
}
