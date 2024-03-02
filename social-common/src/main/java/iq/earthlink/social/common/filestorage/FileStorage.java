package iq.earthlink.social.common.filestorage;

import io.minio.StatObjectResponse;
import iq.earthlink.social.exception.RestApiException;
import org.springframework.http.HttpStatus;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorage {

  /**
   * Returns the storage type of this storage.
   */
  StorageType getStorageType();

  /**
   * Returns the storage type of this storage.
   */
  String getBucketName();

  /**
   * Uploads the file into this file storage using destination path.
   *
   * @param file the file to be uploaded
   * @param path the destination path in file storage
   * @throws FileStorageException if any unexpected error occurred
   */
  void upload(MultipartFile file, String path);

  void upload(InputStream inputStream, long size, String contentType, String fileName, String path);

  /**
   * Downloads the file using provided path and returns content stream of the file.
   *
   * @param path the requested file path
   * @throws FileStorageException if any unexpected error occurred
   */
  InputStream download(String path);

  InputStream download(String path, Long offset, Long length);

  String getUrl(String path);

  String getPresignedUrlForUpload(String fileName);

  String getPresignedUrl(String fileName);

  String getPresignedUrl(String fileName, String bucket);

  StatObjectResponse getObjectData(String path);

  void delete(String path);

  default String getContentHash(MultipartFile file) {
    try {
      return DigestUtils.md5DigestAsHex(file.getInputStream());
    } catch (Exception ex) {
      throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed calculate md5 hash for the file: " + file.getOriginalFilename(), ex);
    }
  }
}
