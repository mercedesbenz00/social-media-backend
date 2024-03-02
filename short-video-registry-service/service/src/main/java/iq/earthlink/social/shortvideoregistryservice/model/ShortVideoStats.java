package iq.earthlink.social.shortvideoregistryservice.model;

import lombok.*;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("short_video_stats")
public class ShortVideoStats {

    @PrimaryKey
    @Column("short_video_id")
    @CassandraType(type = CassandraType.Name.UUID)
    private UUID id;
    
    @Column("likes")
    @CassandraType(type = CassandraType.Name.COUNTER)
    private Long likes;

    @Column("dislikes")
    @CassandraType(type = CassandraType.Name.COUNTER)
    private Long dislikes;

    @Column("views")
    @CassandraType(type = CassandraType.Name.COUNTER)
    private Long views;
    
    @Column("skips")
    @CassandraType(type = CassandraType.Name.COUNTER)
    private Long skips;

    @Column("halves")
    @CassandraType(type = CassandraType.Name.COUNTER)
    private Long halves;

    @Column("comments")
    @CassandraType(type = CassandraType.Name.COUNTER)
    private Long comments;
}

