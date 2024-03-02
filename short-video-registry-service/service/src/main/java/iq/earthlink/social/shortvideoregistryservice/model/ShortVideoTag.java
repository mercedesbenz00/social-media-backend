package iq.earthlink.social.shortvideoregistryservice.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.cassandra.core.mapping.Table;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

//@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString(of = {"id", "name"})
@Table(value = "short_video_tags")
public class ShortVideoTag {

//    @Id
    @PrimaryKey
//    @GenericGenerator(name = "uuid2", strategy = "uuid2")
//    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "uuid2")
//    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

//    @NotNull
    private String externalId;

//    @NotNull
    private String name;


//    @NotNull
    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;
}
