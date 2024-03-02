package iq.earthlink.social.postservice.post.comment;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonMediaFileTranscoded;
import iq.earthlink.social.classes.data.dto.JsonSizedImage;
import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.common.file.*;
import iq.earthlink.social.common.file.rest.AbstractMediaService;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioProperties;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.common.util.FileUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.postservice.event.KafkaProducerService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static iq.earthlink.social.common.util.CommonConstants.POST_SERVICE;
import static java.util.stream.Collectors.groupingBy;

@Service
public class DefaultCommentMediaService extends AbstractMediaService implements CommentMediaService {


    private final CommentFileProperties commentFileProperties;
    private final KafkaProducerService kafkaProducerService;

    public DefaultCommentMediaService(MediaFileRepository fileRepository,
                                      MediaFileTranscodedRepository fileTranscodedRepository,
                                      SizedImageRepository sizedImageRepository,
                                      FileStorageProvider fileStorageProvider,
                                      MinioProperties minioProperties,
                                      CommentFileProperties commentFileProperties, KafkaProducerService kafkaProducerService) {
        super(fileRepository, fileTranscodedRepository, sizedImageRepository, fileStorageProvider, minioProperties);
        this.commentFileProperties = commentFileProperties;
        this.kafkaProducerService = kafkaProducerService;
    }


    @Override
    public void uploadCommentFile(Long commentId, MultipartFile file) {
        removeCommentFile(commentId);
        if (FileUtil.isImage(file) || FileUtil.isVideo(file)) {
            try {
                FileUtil.validateImageOrVideoFiles(Collections.singletonList(file),
                        1, Long.parseLong(commentFileProperties.getImageMaxSize()),
                        1, Long.parseLong(commentFileProperties.getVideoMaxSize()));

                MultipartFile[] arrayFromFile = {file};

                List<MediaFile> mediaFiles = uploadFiles(commentId, MediaFileType.COMMENT_MEDIA, arrayFromFile);

                mediaFiles.stream()
                        .filter(mediaFile -> mediaFile.getMimeType().startsWith("image"))
                        .forEach(mediaFile ->
                                kafkaProducerService.sendMessage(CommonConstants.MEDIA_TO_PROCESS, getImageProcessRequest(mediaFile, POST_SERVICE)));
            } catch (MaxUploadSizeExceededException ex) {
                throw new BadRequestException("error.file.upload.size.exceeded", ex.getMaxUploadSize());
            }
        } else {
            throw new BadRequestException("invalid.image.or.video.file");
        }
    }

    @Override
    public JsonMediaFile findCommentFileWithFullPath(Long commentId) {
        List<MediaFile> mediaFiles = fileRepository.findByOwnerIdAndFileType(commentId, MediaFileType.COMMENT_MEDIA);

        if (mediaFiles.isEmpty()) {
            return null;
        }
        MediaFile file = mediaFiles.get(0);
        JsonMediaFile jsonMediaFile = JsonMediaFile
                .builder()
                .id(file.getId())
                .size(file.getSize())
                .fileType(file.getFileType())
                .createdAt(file.getCreatedAt())
                .mimeType(file.getMimeType())
                .path(file.getPath())
                .ownerId(file.getOwnerId())
                .build();
        if (!CollectionUtils.isEmpty(file.getSizedImages())) {
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
            jsonMediaFileTranscoded.setPath(getFileUrl(transcodedFile.getStorageType(), transcodedFile.getPath()));
            jsonMediaFile.setTranscodedFile(jsonMediaFileTranscoded);
        }
        jsonMediaFile.setPath(getCommentFileUrl(file));

        return jsonMediaFile;
    }

    @Override
    public String getCommentFileUrl(MediaFile file) {
        if (file.getFileType() != MediaFileType.COMMENT_MEDIA) {
            throw new IllegalStateException();
        }

        return getFileUrl(file);
    }

    @Override
    public void removeCommentFile(Long commentId) {
        removeFiles(commentId, MediaFileType.COMMENT_MEDIA);
    }

}
