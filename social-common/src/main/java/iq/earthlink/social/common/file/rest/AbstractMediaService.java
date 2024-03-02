package iq.earthlink.social.common.file.rest;

import iq.earthlink.social.classes.data.dto.*;
import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.common.file.*;
import iq.earthlink.social.common.filestorage.FileStorage;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

import static iq.earthlink.social.common.util.Preconditions.checkNotNull;
import static java.util.UUID.randomUUID;
import static org.springframework.util.CollectionUtils.isEmpty;

public abstract class AbstractMediaService {
    public static final String ERROR_CHECK_NOT_NULL = "error.check.not.null";
    protected final MediaFileRepository fileRepository;
    protected final MediaFileTranscodedRepository fileTranscodedRepository;
    protected final SizedImageRepository sizedImageRepository;
    protected final FileStorageProvider fileStorageProvider;
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final MinioProperties minioProperties;

    protected AbstractMediaService(
            MediaFileRepository fileRepository,
            MediaFileTranscodedRepository fileTranscodedRepository,
            SizedImageRepository sizedImageRepository,
            FileStorageProvider fileStorageProvider,
            MinioProperties minioProperties) {
        this.fileRepository = fileRepository;
        this.fileTranscodedRepository = fileTranscodedRepository;
        this.sizedImageRepository = sizedImageRepository;
        this.fileStorageProvider = fileStorageProvider;
        this.minioProperties = minioProperties;

    }

    protected List<MediaFile> findFiles(Long ownerId, MediaFileType fileType) {
        return fileRepository.findByOwnerIdAndFileType(ownerId, fileType);
    }

    protected Optional<MediaFile> findFile(Long ownerId, MediaFileType fileType) {
        return findFiles(ownerId, fileType).stream().findFirst();
    }

    protected List<MediaFile> uploadFiles(Long ownerId, MediaFileType fileType, MultipartFile[] files) {
        checkNotNull(ownerId, ERROR_CHECK_NOT_NULL, "ownerId");
        checkNotNull(fileType, ERROR_CHECK_NOT_NULL, "fileType");
        checkNotNull(files, ERROR_CHECK_NOT_NULL, "files");

        List<MediaFile> uploadedFiles = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            MediaFile fileRecord = uploadFile(ownerId, fileType, file);
            uploadedFiles.add(fileRecord);
        }

        return uploadedFiles;
    }

    protected MediaFile uploadFile(Long ownerId, MediaFileType fileType, MultipartFile file) {
        checkNotNull(ownerId, ERROR_CHECK_NOT_NULL, "ownerId");
        checkNotNull(fileType, ERROR_CHECK_NOT_NULL, "fileType");
        checkNotNull(file, ERROR_CHECK_NOT_NULL, "file");

        String path = randomUUID().toString();
        FileStorage storage = fileStorageProvider.getStorage(StorageType.MINIO);
        storage.upload(file, path);

        MediaFile fileRecord = MediaFile.builder()
                .ownerId(ownerId)
                .fileType(fileType)
                .mimeType(file.getContentType())
                .path(path)
                .storageType(storage.getStorageType())
                .size(file.getSize())
                .state(MediaFileState.CREATED)
                .build();
        MediaFile fileMedia = fileRepository.save(fileRecord);
        LOGGER.info("Successfully uploaded new file: {}", fileMedia);

        return fileMedia;
    }

    protected InputStream downloadFile(MediaFile file) {
        return downloadFile(file, null, null);
    }

    protected InputStream downloadFile(MediaFile file, Long offset, Long length) {
        checkNotNull(file, ERROR_CHECK_NOT_NULL, "file");

        return fileStorageProvider
                .getStorage(file.getStorageType())
                .download(file.getPath(), offset, length);
    }

    protected String getFileUrl(MediaFile file) {
        checkNotNull(file, ERROR_CHECK_NOT_NULL, "file");
        return fileStorageProvider
                .getStorage(file.getStorageType())
                .getUrl(file.getPath());
    }

    protected String getFileUrl(StorageType storageType, String path) {
        checkNotNull(storageType, ERROR_CHECK_NOT_NULL, "storageType");
        checkNotNull(path, ERROR_CHECK_NOT_NULL, "path");
        return fileStorageProvider
                .getStorage(storageType)
                .getUrl(path);
    }

    public void uploadSizedImages(JsonImageProcessResult result) {
        if (result.getStatus().equals("failed")) {
            Optional<MediaFile> file = fileRepository.findByPath(result.getEntityId());
            if (file.isPresent()) {
                file.get().setState(MediaFileState.OPTIMIZATION_FAILED);
                fileRepository.save(file.get());
            }
        } else {
            String fileId = result.getEntityId();
            Optional<MediaFile> file = fileRepository.findByPath(fileId);
            if (file.isPresent() && file.get().getSizedImages().isEmpty()) {
                var originalImage = file.get();
                Set<SizedImage> sizedImages = new HashSet<>();
                result.getImages().forEach(sizedImage -> sizedImages.add(
                        SizedImage
                                .builder()
                                .size(sizedImage.getSize())
                                .imageSizeType(ImageSizeType.getByRate(sizedImage.getResolution())
                                        .orElse(ImageSizeType.ORIGINAL))
                                .mimeType(sizedImage.getContentType())
                                .storageType(StorageType.MINIO)
                                .path(sizedImage.getObjectName())
                                .createdAt(new Date())
                                .build()));
                originalImage.setSizedImages(sizedImages);
                originalImage.setState(MediaFileState.OPTIMIZED);
                fileRepository.save(originalImage);
            }
        }
    }

    protected JsonImageProcessRequest getImageProcessRequest(MediaFile file, String serviceName) {
        FileStorage storage = fileStorageProvider.getStorage(file.getStorageType());
        JsonImageProcessRequest request = JsonImageProcessRequest
                .builder()
                .entityId(file.getPath())
                .parentEntityId(file.getOwnerId().toString())
                .isFromBucket(true)
                .serviceName(serviceName)
                .fromBucket(JsonStorageDetails
                        .builder()
                        .storageUrl(minioProperties.getEndpoint())
                        .bucketName(storage.getBucketName())
                        .objectName(file.getPath())
                        .accessKeyId(minioProperties.getAccessKey())
                        .secretAccessKey(minioProperties.getSecretKey())
                        .useSsl(minioProperties.getEndpoint().startsWith("https"))
                        .build())
                .toBucket(JsonStorageDetails
                        .builder()
                        .storageUrl(minioProperties.getEndpoint())
                        .bucketName(storage.getBucketName())
                        .accessKeyId(minioProperties.getAccessKey())
                        .secretAccessKey(minioProperties.getSecretKey())
                        .useSsl(minioProperties.getEndpoint().startsWith("https"))
                        .build())
                .build();

        List<JsonImageParameters> imageParameters = new ArrayList<>();
        Arrays.stream(ImageSizeType.values()).forEach(size -> {
            if (size.getRate() > 0) {
                JsonImageParameters webp = JsonImageParameters
                        .builder()
                        .width(size.getRate())
                        .smart(true)
                        .filters(List.of(JsonImageFilter
                                .builder()
                                .name("format")
                                .args("webp")
                                .build()))
                        .build();

                JsonImageParameters jpg = JsonImageParameters
                        .builder()
                        .width(size.getRate())
                        .smart(true)
                        .filters(List.of(JsonImageFilter.builder()
                                .name("format")
                                .args("jpg")
                                .build()))
                        .build();

                imageParameters.add(webp);
                imageParameters.add(jpg);
            }

        });

        request.setImageProcessParams(imageParameters);

        return request;
    }

    protected void removeFiles(Long ownerId, MediaFileType fileType) {
        List<MediaFile> files = fileRepository.findByOwnerIdAndFileType(ownerId, fileType);
        if (isEmpty(files)) {
            return;
        }
        List<Long> transcodedFileIds = files.stream().filter(f -> f.getTranscodedFile() != null)
                .map(f -> f.getTranscodedFile().getId()).toList();

        if (!isEmpty(transcodedFileIds)) {
            List<MediaFileTranscoded> transcodedFiles = fileTranscodedRepository.findByIdIn(transcodedFileIds);
            fileTranscodedRepository.deleteAll(transcodedFiles);
        }

        List<Long> sizedImageIds = files.stream().map(MediaFile::getSizedImages)
                .filter(Objects::nonNull).flatMap(Set::stream).map(SizedImage::getId).toList();

        if (!isEmpty(sizedImageIds)) {
            sizedImageRepository.deleteAllByIdIn(sizedImageIds);
        }

        fileRepository.deleteAll(files);

        files.forEach(file -> {
            try {
                FileStorage storage = fileStorageProvider.getStorage(file.getStorageType());
                storage.delete(file.getPath());
                if (file.getTranscodedFile() != null) {
                    storage.delete(file.getTranscodedFile().getPath());
                    LOGGER.debug("Removed transcoded file: {}", file.getTranscodedFile());
                }

                if (!file.getSizedImages().isEmpty()) {
                    file.getSizedImages().forEach(sizedImage -> storage.delete(sizedImage.getPath()));
                }
                LOGGER.debug("Removed file: {}", file);
            } catch (Exception ex) {
                LOGGER.error("Unable remove file: {}", file, ex);
            }
        });
    }

    protected void removeFile(Long ownerId, Long fileId) {
        Optional<MediaFile> file = fileRepository.findById(fileId);
        if (file.isPresent() && file.get().getOwnerId().equals(ownerId)) {
            var mediaFile = file.get();
            fileRepository.delete(mediaFile);
            removeFileFromStorage(mediaFile);
            LOGGER.debug("Removed file: {}", file);
        }
    }


    protected void removeFileFromStorage(MediaFile file) {
        if (file != null) {
            FileStorage storage = fileStorageProvider.getStorage(file.getStorageType());
            if (file.getTranscodedFile() != null) {
                storage.delete(file.getTranscodedFile().getPath());
            }
            if (!CollectionUtils.isEmpty(file.getSizedImages())) {
                file.getSizedImages().forEach(sizedImage -> storage.delete(sizedImage.getPath()));
            }
            storage.delete(file.getPath());
        }
    }
}
