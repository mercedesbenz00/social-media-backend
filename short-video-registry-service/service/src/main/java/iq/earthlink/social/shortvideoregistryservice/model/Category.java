package iq.earthlink.social.shortvideoregistryservice.model;

import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("category")
public class Category {

    @PrimaryKeyColumn(name = "id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    @CassandraType(type = CassandraType.Name.UUID)
    private UUID id;

    @PrimaryKeyColumn(name = "category_name", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    @CassandraType(type = CassandraType.Name.TEXT)
    private String name;

    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    @Column("created_at")
    private Instant createdAt;

    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    @Column("updated_at")
    private Instant updatedAt;
}
