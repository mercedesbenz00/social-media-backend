package iq.earthlink.social.shortvideoregistryservice.model;

import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("short_videos_of_friends")
public class ShortVideosOfFriends implements Serializable {

    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    @CassandraType(type = CassandraType.Name.BIGINT)
    private long userId;

    @PrimaryKeyColumn(name = "year", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    @CassandraType(type = CassandraType.Name.INT)
    private int year;

    @PrimaryKeyColumn(name = "author_user_name", ordinal = 2, type = PrimaryKeyType.PARTITIONED)
    @CassandraType(type = CassandraType.Name.TEXT)
    private String authorUserName;

    @PrimaryKeyColumn(name = "created_at", ordinal = 3, type = PrimaryKeyType.CLUSTERED)
    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    private Timestamp createdAt;

    @Column("updated_at")
    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    private Timestamp updatedAt;

    @Column("short_video_id")
    @CassandraType(type = CassandraType.Name.UUID)
    private UUID id;

    @Column("author_id")
    @CassandraType(type = CassandraType.Name.BIGINT)
    private long authorId;

    @Column("title")
    @CassandraType(type = CassandraType.Name.TEXT)
    private String title;

    @Column("categories")
    @CassandraType(type = CassandraType.Name.SET, typeArguments = { CassandraType.Name.UUID })
    private Set<UUID> categories = new HashSet<>();

    @Column("metadata")
    @CassandraType(type = CassandraType.Name.MAP, typeArguments = {CassandraType.Name.TEXT, CassandraType.Name.TEXT})
    private Map<String, String> metadata;

    @Column("bucket")
    @CassandraType(type = CassandraType.Name.TEXT)
    private String bucket;

    @Column("privacy_level")
    @Enumerated(EnumType.STRING)
    @CassandraType(type = CassandraType.Name.TEXT)
    private PrivacyLevel privacyLevel;

    @Column("comments_allowed")
    private Boolean commentsAllowed;

    @Column("selected_groups")
    @CassandraType(type = CassandraType.Name.SET, typeArguments = { CassandraType.Name.BIGINT })
    private Set<Long> selectedGroups = new HashSet<>();

    @Column("selected_users")
    @CassandraType(type = CassandraType.Name.SET, typeArguments = { CassandraType.Name.BIGINT })
    private Set<Long> selectedUsers  = new HashSet<>();
}