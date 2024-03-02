package iq.earthlink.social.common.file;

import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.common.filestorage.StorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
    @Index(name = "unique_owner_transcoded_file_idx", columnList = "ownerId,fileType,path")
})
@EntityListeners(AuditingEntityListener.class)
public class MediaFileTranscoded implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_file_transcoded_seq_gen")
  @SequenceGenerator(name = "media_file_transcoded_seq_gen", sequenceName = "media_file_transcoded_seq_gen", allocationSize = 1)
  private Long id;

  @NotEmpty
  @Column(nullable = false)
  private String path;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StorageType storageType;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MediaFileType fileType;

  @NotEmpty
  @Column(nullable = false)
  private String mimeType;

  @NotNull
  @CreatedDate
  @Column(nullable = false)
  private Date createdAt;

  @NotNull
  @Column(nullable = false)
  private Long ownerId;

  @Column(nullable = false)
  private long size;
}
