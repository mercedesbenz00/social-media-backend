package iq.earthlink.social.shortvideoregistryservice.model.migration;

import java.io.Serializable;
import java.util.Objects;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

@PrimaryKeyClass
public class SchemaMigrationKey implements Serializable {

    private static final long serialVersionUID = 6691482812022511000L;

    @PrimaryKeyColumn(name = "applied_successful", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private boolean appliedSuccessful;

    @PrimaryKeyColumn(name = "version", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private int version;

    public SchemaMigrationKey() {
    }

    public SchemaMigrationKey(boolean appliedSuccessful, int version) {
        this.appliedSuccessful = appliedSuccessful;
        this.version = version;
    }

    public boolean isAppliedSuccessful() {
        return appliedSuccessful;
    }

    public int getVersion() {
        return version;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SchemaMigrationKey that = (SchemaMigrationKey) o;
        return appliedSuccessful == that.appliedSuccessful &&
                version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appliedSuccessful, version);
    }
}
