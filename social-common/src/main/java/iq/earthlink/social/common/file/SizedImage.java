package iq.earthlink.social.common.file;

import iq.earthlink.social.classes.data.dto.ImageSizeType;
import iq.earthlink.social.common.filestorage.StorageType;
import lombok.*;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of={"id", "imageSizeType"})
public class SizedImage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sized_image_seq_gen")
    @SequenceGenerator(name = "media_file_seq_gen", sequenceName = "sized_image_seq_gen", allocationSize = 1)
    private Long id;

    /**
     * Possible values:
     * original
     * small
     * medium
     * big
     * xl
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    private ImageSizeType imageSizeType;

    @NotEmpty
    @Column(nullable = false)
    private String path;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StorageType storageType;

    @NotEmpty
    @Column(nullable = false)
    private String mimeType;

    @NotNull
    @CreatedDate
    @Column(nullable = false)
    private Date createdAt;

    @Column(nullable = false)
    private long size;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SizedImage that = (SizedImage) o;
        return id != null && Objects.equals(id, that.id) && Objects.equals(getPath(), that.getPath()) && Objects.equals(getMimeType(), that.getMimeType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath().concat(getMimeType()));
    }
}
