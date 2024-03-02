package iq.earthlink.social.shortvideoregistryservice.repository.migration;

import iq.earthlink.social.shortvideoregistryservice.model.migration.SchemaMigration;
import iq.earthlink.social.shortvideoregistryservice.model.migration.SchemaMigrationKey;
import org.springframework.data.repository.CrudRepository;

public interface SchemaMigrationRepository extends CrudRepository<SchemaMigration, SchemaMigrationKey> {

}
