package iq.earthlink.social.groupservice.category.media;

import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.file.MediaFileRepository;
import iq.earthlink.social.common.file.MediaFileTranscodedRepository;
import iq.earthlink.social.common.file.SizedImageRepository;
import iq.earthlink.social.common.file.rest.AbstractMediaService;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioProperties;
import iq.earthlink.social.common.util.FileUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.groupservice.category.Category;
import iq.earthlink.social.groupservice.category.CategoryMediaService;
import iq.earthlink.social.groupservice.category.repository.CategoryRepository;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Optional;

import static iq.earthlink.social.common.util.CommonConstants.GROUP_SERVICE;
import static iq.earthlink.social.common.util.CommonConstants.MEDIA_TO_PROCESS;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
public class DefaultCategoryMediaService extends AbstractMediaService implements CategoryMediaService {

    CategoryRepository categoryRepository;
    private final KafkaProducerService kafkaProducerService;

    public DefaultCategoryMediaService(
            MediaFileRepository fileRepository,
            MediaFileTranscodedRepository fileTranscodedRepository,
            SizedImageRepository sizedImageRepository,
            FileStorageProvider fileStorageProvider,
            MinioProperties minioProperties,
            CategoryRepository categoryRepository, KafkaProducerService kafkaProducerService) {
        super(fileRepository, fileTranscodedRepository, sizedImageRepository, fileStorageProvider, minioProperties);
        this.categoryRepository = categoryRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Nonnull
    @Override
    public MediaFile uploadAvatar(Category category, MultipartFile file, Boolean isAdmin) {
        checkNotNull(category, "error.check.not.null", "category");
        checkPermissions(isAdmin);
        checkContentType(file);

        removeFiles(category.getId(), MediaFileType.CATEGORY_AVATAR);
        MediaFile avatar = uploadFile(category.getId(), MediaFileType.CATEGORY_AVATAR, file);
        category.setAvatar(avatar);
        categoryRepository.save(category);

        if (avatar.getMimeType().startsWith("image/")) {
            kafkaProducerService.sendMessage(MEDIA_TO_PROCESS, getImageProcessRequest(avatar, GROUP_SERVICE));
        }
        return category.getAvatar();
    }

    @Nonnull
    @Override
    public Optional<MediaFile> findAvatar(Long categoryId) {
        return findFile(categoryId, MediaFileType.CATEGORY_AVATAR);
    }

    @Override
    public InputStream downloadAvatar(MediaFile file) {
        if (file.getFileType() != MediaFileType.CATEGORY_AVATAR) {
            throw new IllegalStateException("error.wrong.fileType");
        }
        return downloadFile(file);
    }

    @Override
    @Transactional
    public void removeAvatar(Category category, Boolean isAdmin) {
        checkPermissions(isAdmin);
        removeFiles(category.getId(), MediaFileType.CATEGORY_AVATAR);
        category.setAvatar(null);
        categoryRepository.save(category);
    }

    @Transactional
    @Nonnull
    @Override
    public MediaFile uploadCover(Category category, MultipartFile file, Boolean isAdmin) {
        checkNotNull(category, "error.check.not.null", "category");
        checkPermissions(isAdmin);
        checkContentType(file);

        removeFiles(category.getId(), MediaFileType.CATEGORY_COVER);
        MediaFile cover = uploadFile(category.getId(), MediaFileType.CATEGORY_COVER, file);
        category.setCover(cover);
        categoryRepository.save(category);
        if (cover.getMimeType().startsWith("image/")) {
            kafkaProducerService.sendMessage(MEDIA_TO_PROCESS, getImageProcessRequest(cover, GROUP_SERVICE));
        }
        return category.getCover();
    }

    @Nonnull
    @Override
    public Optional<MediaFile> findCover(Long categoryId) {
        return findFile(categoryId, MediaFileType.CATEGORY_COVER);
    }

    @Override
    public InputStream downloadCover(MediaFile file) {
        if (file.getFileType() != MediaFileType.CATEGORY_COVER) {
            throw new IllegalStateException("error.wrong.fileType");
        }
        return downloadFile(file);
    }

    @Override
    @Transactional
    public void removeCover(Category category, Boolean isAdmin) {
        checkPermissions(isAdmin);
        removeFiles(category.getId(), MediaFileType.CATEGORY_COVER);
        category.setCover(null);
        categoryRepository.save(category);
    }

    @Override
    public String getFileURL(MediaFile file) {
        return super.getFileUrl(file);
    }

    @Override
    public String getFileURL(StorageType storageType, String path) {
        return super.getFileUrl(storageType, path);
    }

    private void checkPermissions(boolean isAdmin) {
        if (!isAdmin) throw new ForbiddenException("error.person.unauthorized.to.update.category");
    }

    private static void checkContentType(MultipartFile file) {
        if (!FileUtil.isImage(file)) {
            throw new BadRequestException("invalid.image.file");
        }
    }
}
