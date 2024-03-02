package iq.earthlink.social.groupservice.category;

import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.filestorage.StorageType;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Optional;

public interface CategoryMediaService {

  @Nonnull
  MediaFile uploadAvatar(Category category, MultipartFile avatar, Boolean isAdmin);

  @Nonnull
  Optional<MediaFile> findAvatar(Long categoryId);

  InputStream downloadAvatar(MediaFile file);

  void removeAvatar(Category category, Boolean isAdmin);

  @Nonnull
  MediaFile uploadCover(Category category, MultipartFile cover, Boolean isAdmin);

  @Nonnull
  Optional<MediaFile> findCover(Long categoryId);

  InputStream downloadCover(MediaFile file);

  void removeCover(Category category, Boolean isAdmin);

  String getFileURL(MediaFile file);

  String getFileURL(StorageType storageType, String path);

}
