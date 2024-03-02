package iq.earthlink.social.postservice.post.comment;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.common.file.MediaFile;
import org.springframework.web.multipart.MultipartFile;

public interface CommentMediaService {

  void uploadCommentFile(Long commentId, MultipartFile file);

  JsonMediaFile findCommentFileWithFullPath(Long commentId);

  String getCommentFileUrl(MediaFile file);

  void removeCommentFile(Long commentId);
}
