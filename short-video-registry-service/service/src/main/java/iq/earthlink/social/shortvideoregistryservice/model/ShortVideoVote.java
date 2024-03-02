package iq.earthlink.social.shortvideoregistryservice.model;

import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("short_video_vote")
public class ShortVideoVote {
    @Column("person_id")
    @PrimaryKeyColumn(name = "person_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    @CassandraType(type = CassandraType.Name.BIGINT)
    private long personId;

    @PrimaryKeyColumn(name = "short_video_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    @Column("short_video_id")
    @CassandraType(type = CassandraType.Name.UUID)
    private UUID id;

    @Column("vote_type")
    @CassandraType(type = CassandraType.Name.INT)
    private int voteType;

    @Column("created_at")
    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    private Timestamp createdAt;
}
