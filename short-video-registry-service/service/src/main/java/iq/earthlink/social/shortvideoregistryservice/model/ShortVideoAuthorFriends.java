package iq.earthlink.social.shortvideoregistryservice.model;

import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("short_video_author_friends")
public class ShortVideoAuthorFriends {

    @PrimaryKeyColumn(name = "author_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    @CassandraType(type = CassandraType.Name.BIGINT)
    private Long authorId;

    @Column("friend_user_id")
    @PrimaryKeyColumn(name = "friend_user_id", ordinal = 0, type = PrimaryKeyType.CLUSTERED)
    @CassandraType(type = CassandraType.Name.TEXT)
    private Long friendUserId;

    @Column("author_username")
    @CassandraType(type = CassandraType.Name.TEXT)
    private String authorUsername;

}
