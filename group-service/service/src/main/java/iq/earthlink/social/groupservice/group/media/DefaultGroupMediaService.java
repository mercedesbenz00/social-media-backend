package iq.earthlink.social.groupservice.group.media;

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
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import iq.earthlink.social.groupservice.group.GroupMediaService;
import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.GroupRepository;
import iq.earthlink.social.groupservice.group.UserGroup;
import iq.earthlink.social.groupservice.group.member.GroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMemberRepository;
import iq.earthlink.social.groupservice.group.permission.repository.GroupPermissionRepository;
import iq.earthlink.social.groupservice.group.rest.UserGroupDto;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.*;

import static iq.earthlink.social.common.util.CommonConstants.GROUP_SERVICE;
import static iq.earthlink.social.common.util.CommonConstants.MEDIA_TO_PROCESS;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;
import static java.util.stream.Collectors.groupingBy;

@Service
public class DefaultGroupMediaService extends AbstractMediaService implements GroupMediaService {
    private final GroupMemberRepository groupMemberRepository;
    private final GroupPermissionRepository permissionRepository;
    private final GroupRepository groupRepository;
    private final KafkaProducerService kafkaProducerService;

    public DefaultGroupMediaService(
            MediaFileRepository fileRepository,
            MediaFileTranscodedRepository fileTranscodedRepository,
            FileStorageProvider fileStorageProvider,
            SizedImageRepository sizedImageRepository,
            GroupPermissionRepository permissionRepository,
            MinioProperties minioProperties,
            GroupMemberRepository groupMemberRepository,
            GroupRepository groupRepository, KafkaProducerService kafkaProducerService) {
        super(fileRepository, fileTranscodedRepository, sizedImageRepository, fileStorageProvider, minioProperties);
        this.groupMemberRepository = groupMemberRepository;
        this.permissionRepository = permissionRepository;
        this.groupRepository = groupRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Transactional
    @Nonnull
    @Override
    public MediaFile uploadAvatar(UserGroup group, MultipartFile file, Long currentUserId, Boolean isCurrentUserAdmin) {
        checkNotNull(group, "error.check.not.null", "group");
        checkPermissions(group.getId(), currentUserId, isCurrentUserAdmin);
        checkContentType(file);

        removeFiles(group.getId(), MediaFileType.GROUP_AVATAR);
        MediaFile avatar = uploadFile(group.getId(), MediaFileType.GROUP_AVATAR, file);
        group.setAvatar(avatar);
        groupRepository.save(group);

        if (avatar.getMimeType().startsWith("image/")) {
            kafkaProducerService.sendMessage(MEDIA_TO_PROCESS, getImageProcessRequest(avatar, GROUP_SERVICE));
        }
        var userGroupDTO = UserGroupDto
                .builder()
                .id(group.getId())
                .avatar(JsonMediaFile
                        .builder()
                        .id(avatar.getId())
                        .path(avatar.getPath())
                        .fileType(avatar.getFileType())
                        .size(avatar.getSize())
                        .mimeType(avatar.getMimeType())
                        .createdAt(avatar.getCreatedAt())
                        .build())
                .build();
        kafkaProducerService.sendMessage(CommonConstants.GROUP_UPDATED, userGroupDTO);
        return group.getAvatar();
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
            jsonMediaFile.setPath(getFileUrl(file));
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
            jsonMediaFiles.add(jsonMediaFile);
        });

        return jsonMediaFiles;
    }

    @Nonnull
    @Override
    public Optional<MediaFile> findAvatar(Long groupId) {
        return findFile(groupId, MediaFileType.GROUP_AVATAR);
    }

    @Override
    public InputStream downloadAvatar(MediaFile file) {
        if (file.getFileType() != MediaFileType.GROUP_AVATAR) {
            throw new IllegalStateException("error.wrong.fileType");
        }
        return downloadFile(file);
    }

    @Override
    @Transactional
    public void removeAvatar(UserGroup group, Long currentUserId, Boolean isCurrentUserAdmin) {
        checkPermissions(group.getId(), currentUserId, isCurrentUserAdmin);
        removeFiles(group.getId(), MediaFileType.GROUP_AVATAR);
        group.setAvatar(null);
        var userGroupDTO = UserGroupDto
                .builder()
                .id(group.getId())
                .avatar(null)
                .build();
        kafkaProducerService.sendMessage(CommonConstants.GROUP_UPDATED, userGroupDTO);
        groupRepository.save(group);
    }

    @Transactional
    @Nonnull
    @Override
    public MediaFile uploadCover(UserGroup group, MultipartFile file, Long currentUserId, Boolean isCurrentUserAdmin) {
        checkNotNull(group, "error.check.not.null", "group");
        checkPermissions(group.getId(), currentUserId, isCurrentUserAdmin);
        checkContentType(file);

        removeFiles(group.getId(), MediaFileType.GROUP_COVER);
        MediaFile cover = uploadFile(group.getId(), MediaFileType.GROUP_COVER, file);
        group.setCover(cover);
        groupRepository.save(group);

        if (cover.getMimeType().startsWith("image/")) {
            kafkaProducerService.sendMessage(MEDIA_TO_PROCESS, getImageProcessRequest(cover, GROUP_SERVICE));
        }
        return group.getCover();
    }

    @Nonnull
    @Override
    public Optional<MediaFile> findCover(Long groupId) {
        return findFile(groupId, MediaFileType.GROUP_COVER);
    }

    @Override
    public InputStream downloadCover(MediaFile file) {
        if (file.getFileType() != MediaFileType.GROUP_COVER) {
            throw new IllegalStateException("error.wrong.fileType");
        }
        return downloadFile(file);
    }

    @Override
    @Transactional
    public void removeCover(UserGroup group, Long currentUserId, Boolean isCurrentUserAdmin) {
        checkPermissions(group.getId(), currentUserId, isCurrentUserAdmin);
        removeFiles(group.getId(), MediaFileType.GROUP_COVER);
        group.setCover(null);
        groupRepository.save(group);
    }

    private void checkPermissions(Long groupId, Long currentUserId, boolean isCurrentUserAdmin) {
        GroupMember member = groupMemberRepository.findActiveMember(groupId, currentUserId);
        boolean isGroupAdmin = !Objects.isNull(member) && permissionRepository.existsPermission(currentUserId, groupId, GroupMemberStatus.ADMIN);

        if (!isCurrentUserAdmin && !isGroupAdmin)
            throw new ForbiddenException("error.person.unauthorized.to.update.group");
    }

    private static void checkContentType(MultipartFile file) {
        if (!FileUtil.isImage(file)) {
            throw new BadRequestException("invalid.image.file");
        }
    }
}
