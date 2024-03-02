package iq.earthlink.social.postservice.post;

import iq.earthlink.social.classes.data.dto.JsonImageProcessResult;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.postservice.post.model.Post;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public interface PostMediaService {

  @Nonnull
  List<MediaFile> uploadPostFiles(Post post, MultipartFile[] files);

  @Nonnull
  List<JsonMediaFile> findPostFilesWithFullPath(Long postId);

  List<MediaFile> findPostFiles(Long postId);

  List<JsonMediaFile> findFilesByPostIds(List<Long> postIds);

  List<JsonMediaFile> convertMediaFilesToJsonMediaFiles(List<MediaFile> mediaFiles);

  @Nonnull
  Optional<MediaFile> findPostFile(Long postId, Long fileId);

  ResponseEntity<Resource> downloadPostFile(MediaFile file, String rangeHeader);

  String getPostFileUrl(MediaFile file);

  String getPostFileUrl(MediaFileType fileType, StorageType storageType, String path);

  void removePostFiles(Long postId);
  void removePostFile(Long postId, Long fileId);
  void savePostMediaFiles(List<MediaFile> files, Long postId);
  void uploadSizedImages(Long postId, JsonImageProcessResult result);
  void uploadSizedImages(JsonImageProcessResult result);
}
