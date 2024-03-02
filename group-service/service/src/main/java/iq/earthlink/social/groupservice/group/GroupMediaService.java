package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.classes.data.dto.JsonImageProcessResult;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.common.file.MediaFile;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface GroupMediaService {

  @Nonnull
  MediaFile uploadAvatar(UserGroup group, MultipartFile avatar, Long currentUserId, Boolean isCurrentUserAdmin);

  List<JsonMediaFile> convertMediaFilesToJsonMediaFiles(List<MediaFile> mediaFiles);

  @Nonnull
  Optional<MediaFile> findAvatar(Long groupId);

  InputStream downloadAvatar(MediaFile file);

  void removeAvatar(UserGroup group, Long personId, Boolean isAdmin);

  @Nonnull
  MediaFile uploadCover(UserGroup category, MultipartFile cover, Long currentUserId, Boolean isCurrentUserAdmin);

  @Nonnull
  Optional<MediaFile> findCover(Long groupId);

  InputStream downloadCover(MediaFile file);

  void removeCover(UserGroup group, Long personId, Boolean isAdmin);
  void uploadSizedImages(JsonImageProcessResult result);
}
