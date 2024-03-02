package iq.earthlink.social.common.filestorage.minio;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import iq.earthlink.social.common.filestorage.FileNotFoundException;
import iq.earthlink.social.common.filestorage.FileStorage;
import iq.earthlink.social.common.filestorage.FileStorageException;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.exception.RestApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Component
public class MinioFileStorage implements FileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioFileStorage.class);

    private final MinioProperties properties;
    private final MinioClient client;

    public MinioFileStorage(MinioProperties properties) {
        this.properties = properties;
        this.client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();

        ensureBucketExists(properties.getBucketName());
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.MINIO;
    }

    @Override
    public String getBucketName() {
        return properties.getBucketName();
    }

    @Override
    public void upload(MultipartFile file, String path) {
        try {
            upload(file.getInputStream(), file.getSize(), file.getContentType(), file.getName(), path);
        } catch (IOException e) {
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Failed upload file: %s to Minio storage", file.getName()), e);
        }
    }

    @Override
    public void upload(InputStream inputStream, long size, String contentType, String fileName, String path) {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(path)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build();

            client.putObject(args);
            LOGGER.info("Successfully uploaded file: '{}' to Minio storage", fileName);
        } catch (Exception  ex) {
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Failed upload file: %s to Minio storage", fileName), ex);
        }
    }

    @Override
    public InputStream download(String path) {
        return download(path, null, null);
    }

    @Override
    public InputStream download(String path, Long offset, Long length) {

        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(properties.getBucketName())
                .object(path)
                .offset(offset)
                .length(length)
                .build();

        try {
            return client.getObject(args);
        } catch (ErrorResponseException ex) {
            if (ex.errorResponse().code().equals("NoSuchKey")) {
                throw new FileNotFoundException("Not found file with path: " + path);
            }
            throw new FileStorageException("Unable download file with path: " + path, ex);
        } catch (Exception ex) {
            throw new FileStorageException("Unable download file with path: " + path, ex);
        }
    }

    @Override
    public String getUrl(String path) {
        try {
            StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(path)
                    .build();

            client.statObject(statObjectArgs);

            return UriComponentsBuilder.fromUriString(properties.getExternalEndpoint())
                    .path(properties.getBucketName())
                    .pathSegment(path)
                    .build()
                    .toUriString();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public String getPresignedUrlForUpload(String fileName) {
        try {
            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(properties.getBucketName())
                            .object(fileName)
                            .expiry(1, TimeUnit.DAYS)
                            .build());
        } catch (Exception ex) {
            throw new FileStorageException("Could not get Presigned URL for the file " + fileName);
        }
    }

    @Override
    public String getPresignedUrl(String fileName) {
        try {
            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getBucketName())
                            .object(fileName)
                            .expiry(1, TimeUnit.DAYS)
                            .build());
        } catch (Exception ex) {
            throw new FileStorageException("Could not get Presigned URL for the file " + fileName);
        }
    }


    @Override
    public String getPresignedUrl(String fileName, String bucket) {
        try {
            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(fileName)
                            .expiry(1, TimeUnit.DAYS)
                            .build());
        } catch (Exception ex) {
            throw new FileStorageException("Could not get Presigned URL for the file" + fileName);
        }
    }

    @Override
    public StatObjectResponse getObjectData(String path) {
        try {
            StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(path)
                    .build();

            return client.statObject(statObjectArgs);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void delete(String path) {
        RemoveObjectArgs args = RemoveObjectArgs.builder()
                .bucket(properties.getBucketName())
                .object(path)
                .build();

        try {
            client.removeObject(args);
            LOGGER.info("Removed object with path: {}", path);
        } catch (Exception ex) {
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed remove object: " + path, ex);
        }
    }

    private void ensureBucketExists(String bucketName) {
        LOGGER.trace("Checking that bucket: {} exists", bucketName);
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                LOGGER.trace("{} bucket does not exists. Creating new....", bucketName);
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                LOGGER.trace("{} bucket successfully created", bucketName);
            }
        } catch (Exception ex) {
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Failed checking bucket: %s status", bucketName), ex);
        }
    }
}
