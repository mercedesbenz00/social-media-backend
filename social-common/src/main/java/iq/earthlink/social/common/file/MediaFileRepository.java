package iq.earthlink.social.common.file;

import iq.earthlink.social.classes.enumeration.MediaFileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

  List<MediaFile> findByOwnerIdAndFileType(Long ownerId, MediaFileType fileType);

  List<MediaFile> findByOwnerIdInAndFileType(List<Long> ownerId, MediaFileType fileType);

  Optional<MediaFile> findByPath(String path);
}
