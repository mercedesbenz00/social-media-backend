package iq.earthlink.social.postservice.post;

import iq.earthlink.social.classes.data.dto.JsonImageProcessResult;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonMediaFileTranscoded;
import iq.earthlink.social.classes.data.dto.JsonSizedImage;
import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.common.file.*;
import iq.earthlink.social.common.file.rest.AbstractMediaService;
import iq.earthlink.social.common.file.rest.DownloadUtils;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioProperties;
import iq.earthlink.social.common.util.FileUtil;
import iq.earthlink.social.postservice.event.KafkaProducerService;
import iq.earthlink.social.postservice.post.model.Post;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.util.*;

import static iq.earthlink.social.common.util.CommonConstants.MEDIA_TO_PROCESS;
import static iq.earthlink.social.common.util.CommonConstants.POST_SERVICE;
import static java.util.stream.Collectors.groupingBy;

@Component
public class DefaultPostMediaService extends AbstractMediaService implements PostMediaService {

    private final Integer imageMaxCount;
    private final Long imageMaxSize;
    private final Integer videoMaxCount;
    private final Long videoMaxSize;
    private final KafkaProducerService kafkaProducerService;

    public DefaultPostMediaService(
            MediaFileRepository fileRepository,
            MediaFileTranscodedRepository mediaFileTranscodedRepository,
            SizedImageRepository sizedImageRepository,
            FileStorageProvider fileStorageProvider,
            MinioProperties minioProperties,
            PostFileProperties properties,
            KafkaProducerService kafkaProducerService) {
        super(fileRepository,
                mediaFileTranscodedRepository,
                sizedImageRepository,
                fileStorageProvider,
                minioProperties);

        this.imageMaxCount = Integer.parseInt(properties.getImageMaxCount());
        this.imageMaxSize = Long.parseLong(properties.getImageMaxSize());
        this.videoMaxCount = Integer.parseInt(properties.getVideoMaxCount());
        this.videoMaxSize = Long.parseLong(properties.getVideoMaxSize());
        this.kafkaProducerService = kafkaProducerService;
    }

    @Nonnull
    @Override
    @CacheEvict(value = "spring.cache.post.media", key = "#post.id")
    public List<MediaFile> uploadPostFiles(Post post, MultipartFile[] files) {
        FileUtil.validateImageOrVideoFiles(Arrays.asList(files), imageMaxCount, imageMaxSize, videoMaxCount, videoMaxSize);

        List<MediaFile> mediaFiles = uploadFiles(post.getId(), MediaFileType.POST_MEDIA, files);
        mediaFiles.stream()
                .filter(mediaFile -> mediaFile.getMimeType().startsWith("image"))
                .forEach(mediaFile ->
                        kafkaProducerService.sendMessage(MEDIA_TO_PROCESS, getImageProcessRequest(mediaFile, POST_SERVICE)));
        return mediaFiles;
    }

    @Nonnull
    @Override
    @Cacheable(value = "spring.cache.post.media", key = "#postId")
    public List<JsonMediaFile> findPostFilesWithFullPath(Long postId) {
        List<MediaFile> mediaFiles = fileRepository.findByOwnerIdAndFileType(postId, MediaFileType.POST_MEDIA);
        return convertMediaFilesToJsonMediaFiles(mediaFiles);
    }

    @Nonnull
    @Override
    public List<MediaFile> findPostFiles(Long postId) {
        return fileRepository.findByOwnerIdAndFileType(postId, MediaFileType.POST_MEDIA);
    }

    @Override
    public List<JsonMediaFile> findFilesByPostIds(List<Long> postIds) {
        List<MediaFile> mediaFiles = fileRepository.findByOwnerIdInAndFileType(postIds, MediaFileType.POST_MEDIA);
        return convertMediaFilesToJsonMediaFiles(mediaFiles);
    }

    @Override
    public List<JsonMediaFile> convertMediaFilesToJsonMediaFiles(List<MediaFile> mediaFiles) {
        List<JsonMediaFile> jsonMediaFiles = new ArrayList<>();

        mediaFiles.forEach(file -> {
            // replaced dozer mapping with manual object creation as mapper was 4 times slower
            JsonMediaFile jsonMediaFile = JsonMediaFile
                    .builder()
                    .id(file.getId())
                    .fileType(file.getFileType())
                    .createdAt(file.getCreatedAt())
                    .mimeType(file.getMimeType())
                    .ownerId(file.getOwnerId())
                    .size(file.getSize())
                    .path(file.getPath())
                    .build();
            if (file.getMimeType().contains("image") && CollectionUtils.isNotEmpty(file.getSizedImages())) {
                Map<String, List<JsonSizedImage>> sizedImages = file.getSizedImages().stream()
                        .map(sizedImage -> JsonSizedImage
                                .builder()
                                .imageSizeType(sizedImage.getImageSizeType())
                                .size(sizedImage.getSize())
                                .path(getFileUrl(StorageType.MINIO, sizedImage.getPath()))
                                .createdAt(sizedImage.getCreatedAt())
                                .mimeType(sizedImage.getMimeType())
                                .build()
                        ).collect(groupingBy(image -> image.getMimeType().replace("image/", "")));
                jsonMediaFile.setSizedImages(sizedImages);
            } else {
                jsonMediaFile.setSizedImages(null);
            }
            jsonMediaFile.setPath(getPostFileUrl(file));
            MediaFileTranscoded transcodedFile = file.getTranscodedFile();
            if (transcodedFile != null) {
                JsonMediaFileTranscoded jsonMediaFileTranscoded = JsonMediaFileTranscoded
                        .builder()
                        .id(transcodedFile.getId())
                        .fileType(transcodedFile.getFileType())
                        .path(transcodedFile.getPath())
                        .mimeType(transcodedFile.getMimeType())
                        .ownerId(transcodedFile.getOwnerId())
                        .size(transcodedFile.getSize())
                        .createdAt(transcodedFile.getCreatedAt())
                        .build();
                jsonMediaFileTranscoded.setPath(getPostFileUrl(transcodedFile.getFileType(),
                        transcodedFile.getStorageType(), transcodedFile.getPath()));
                jsonMediaFile.setTranscodedFile(jsonMediaFileTranscoded);
            }
            jsonMediaFiles.add(jsonMediaFile);
        });

        return jsonMediaFiles;
    }

    @Nonnull
    @Override
    public Optional<MediaFile> findPostFile(Long postId, Long fileId) {
        return findPostFiles(postId).stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst();
    }

    @Override
    public ResponseEntity<Resource> downloadPostFile(MediaFile file, String rangeHeader) {
        if (file.getFileType() != MediaFileType.POST_MEDIA) {
            throw new IllegalStateException();
        }

        return DownloadUtils.fileResponse(file,
                downloadFile(file, DownloadUtils.getOffset(rangeHeader), DownloadUtils.getLength(file, rangeHeader)),
                rangeHeader);
    }

    @Override
    public String getPostFileUrl(MediaFile file) {
        return getFileUrl(file);
    }

    @Override
    public String getPostFileUrl(MediaFileType fileType, StorageType storageType, String path) {
        if (fileType != MediaFileType.POST_MEDIA) {
            throw new IllegalStateException();
        }

        return getFileUrl(storageType, path);
    }

    @Override
    @CacheEvict(value = "spring.cache.post.media", key = "#postId")
    public void uploadSizedImages(Long postId, JsonImageProcessResult result) {
        super.uploadSizedImages(result);
    }

    @Override
    public void uploadSizedImages(JsonImageProcessResult result) {
        super.uploadSizedImages(result);
    }

    @Override
    @CacheEvict(value = "spring.cache.post.media", key = "#postId")
    public void removePostFiles(Long postId) {
        removeFiles(postId, MediaFileType.POST_MEDIA);
    }

    @Override
    @CacheEvict(value = "spring.cache.post.media", key = "#postId")
    public void removePostFile(Long postId, Long fileId) {
            removeFile(postId, fileId);
    }

    @Override
    @CacheEvict(value = "spring.cache.post.media", key = "#postId")
    public void savePostMediaFiles(List<MediaFile> files, Long postId) {
        fileRepository.saveAll(files);
    }

}
