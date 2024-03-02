package iq.earthlink.social.personservice.person;

import iq.earthlink.social.classes.data.dto.JsonImageProcessResult;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.personservice.person.model.Person;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Optional;

public interface PersonMediaService {

  @Nonnull
  MediaFile uploadAvatar(Person person, MultipartFile avatar, Person currentUser);

  @Nonnull
  Optional<MediaFile> findAvatar(Long personId);

  InputStream downloadAvatar(MediaFile file);

  void removeAvatar(Person person, Person currentUser);

  @Nonnull
  MediaFile uploadCover(Person person, MultipartFile cover, Person currentUser);

  @Nonnull
  Optional<MediaFile> findCover(Long personId);

  InputStream downloadCover(MediaFile file);

  void removeCover(Person person, Person currentUser);

  String getFileURL(MediaFile file);

  String getFileURL(StorageType storageType, String path);

  void uploadSizedImages(JsonImageProcessResult result);
}
