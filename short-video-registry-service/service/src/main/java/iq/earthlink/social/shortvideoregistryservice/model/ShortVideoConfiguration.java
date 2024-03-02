package iq.earthlink.social.shortvideoregistryservice.model;

import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import lombok.*;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(value="short_video_config")
public class ShortVideoConfiguration {
    @PrimaryKey
    @Column("id")
    @CassandraType(type = CassandraType.Name.UUID)
    private UUID id;

    @Column("person_id")
    @CassandraType(type = CassandraType.Name.BIGINT)
    private long personId;

    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    @Column("created_at")
    private Instant createdAt;

    @Column("privacy_level")
    @Enumerated(EnumType.STRING)
    @CassandraType(type = CassandraType.Name.TEXT)
    private PrivacyLevel privacyLevel;

    @Column("comments_allowed")
    @CassandraType(type = CassandraType.Name.BOOLEAN)
    private Boolean commentsAllowed = true;

    @Column("selected_groups")
    @CassandraType(type = CassandraType.Name.SET, typeArguments = { CassandraType.Name.BIGINT })
    private Set<Long> selectedGroups = new HashSet<>();

    @Column("selected_users")
    @CassandraType(type = CassandraType.Name.SET, typeArguments = { CassandraType.Name.BIGINT })
    private Set<Long> selectedUsers  = new HashSet<>();
}
