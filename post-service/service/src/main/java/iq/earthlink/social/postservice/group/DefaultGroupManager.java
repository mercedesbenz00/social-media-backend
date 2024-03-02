package iq.earthlink.social.postservice.group;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonSizedImage;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.file.MediaFileState;
import iq.earthlink.social.common.file.SizedImage;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.postservice.group.dto.GroupDTO;
import iq.earthlink.social.postservice.group.dto.GroupEventDTO;
import iq.earthlink.social.postservice.group.member.GroupMemberManager;
import iq.earthlink.social.postservice.group.model.UserGroup;
import iq.earthlink.social.postservice.group.repository.GroupRepository;
import iq.earthlink.social.postservice.post.PostMediaService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Service
@Log4j2
@RequiredArgsConstructor
public class DefaultGroupManager implements GroupManager {
    private final GroupRepository repository;
    private final PostMediaService postMediaService;
    private final GroupMemberManager groupMemberManager;

    @Override
    public List<GroupDTO> getGroupsByIds(List<Long> groupIds) {
        return repository.getByGroupIdIn(groupIds).stream()
                .map(group -> {
                    var groupDTO = GroupDTO
                            .builder()
                            .groupId(group.getGroupId())
                            .name(group.getName())
                            .accessType(group.getAccessType())
                            .postingPermission(group.getPostingPermission())
                            .createdAt(group.getCreatedAt())
                            .build();
                    if (group.getAvatar() != null) {
                        var avatar = group.getAvatar();
                        var listJsonMediaFiles = postMediaService.convertMediaFilesToJsonMediaFiles(List.of(avatar));
                        groupDTO.setAvatar(listJsonMediaFiles.get(0));
                    }
                    return groupDTO;
                }).toList();
    }

    @Override
    public GroupDTO getGroupById(Long groupId) {
        var group = repository.getByGroupId(groupId)
                .orElseThrow(() -> new NotFoundException("error.group.not.found.byId", groupId));
        GroupDTO groupDTO = GroupDTO
                .builder()
                .groupId(group.getGroupId())
                .name(group.getName())
                .postingPermission(group.getPostingPermission())
                .accessType(group.getAccessType())
                .createdAt(group.getCreatedAt())
                .build();
        if (group.getAvatar() != null) {
            var avatar = group.getAvatar();
            var listJsonMediaFiles = postMediaService.convertMediaFilesToJsonMediaFiles(List.of(avatar));
            groupDTO.setAvatar(listJsonMediaFiles.get(0));
        }
        return groupDTO;
    }

    @Override
    public void saveGroup(GroupEventDTO groupEventDTO) {
        var existingGroup = repository.getByGroupId(groupEventDTO.getId());
        if(existingGroup.isEmpty()) {
            var groupEntity = generateGroupEntity(groupEventDTO);
            if (groupEntity != null) {
                repository.save(groupEntity);
            }
        }
    }

    @Override
    public void updateGroup(GroupEventDTO groupEventDTO) {
        var group = repository.getByGroupId(groupEventDTO.getId())
                .orElseThrow(() -> new NotFoundException("error.group.not.found.byId", groupEventDTO.getId()));
        group.setName(firstNonNull(groupEventDTO.getName(), group.getName()));
        group.setPostingPermission(firstNonNull(groupEventDTO.getPostingPermission(), group.getPostingPermission()));
        group.setAccessType(firstNonNull(groupEventDTO.getAccessType(), group.getAccessType()));
        if (groupEventDTO.getAvatar() != null && groupEventDTO.getAvatar().getPath() != null) {
            var avatar = generateAvatarEntity(groupEventDTO.getAvatar());
            avatar.setOwnerId(groupEventDTO.getId());
            group.setAvatar(avatar);
        } else {
            group.setAvatar(null);
        }
        repository.save(group);
    }

    @Override
    public List<Long> getMyGroupIds(Long personId, List<Long> groupIds) {
        return repository.getMyGroupIds(personId, groupIds);
    }

    @Override
    public boolean hasAccessToGroup(Long personId, boolean isAdmin, Long groupId) {
        if (isAdmin) {
            return true;
        }
        Optional<Long> accessibleGroup = repository.getAccessibleGroup(personId, groupId);
        return accessibleGroup.isPresent();
    }


    //todo: this method for migration purpose only, delete it after release to EL env
    @Override
    public Long getCount() {
        return repository.count();
    }

    //todo: this method for migration purpose only, delete it after release to EL env
    @Override
    public void saveAllGroups(List<GroupEventDTO> groupEventDTOS) {
        var groups = groupEventDTOS.stream().map(this::generateGroupEntity).toList();
        if (!CollectionUtils.isEmpty(groups)) {
            repository.saveAll(groups);
        }
    }

    private UserGroup generateGroupEntity(GroupEventDTO groupEventDTO) {
        if (groupEventDTO != null) {
            log.info("generating new group entity with id: {}", groupEventDTO.getId());
            UserGroup userGroup = UserGroup
                    .builder()
                    .groupId(groupEventDTO.getId())
                    .name(groupEventDTO.getName())
                    .accessType(groupEventDTO.getAccessType())
                    .postingPermission(groupEventDTO.getPostingPermission())
                    .createdAt(groupEventDTO.getCreatedAt())
                    .build();
            if (groupEventDTO.getAvatar() != null && groupEventDTO.getAvatar().getPath() != null) {
                var avatar = generateAvatarEntity(groupEventDTO.getAvatar());
                avatar.setOwnerId(groupEventDTO.getId());
                userGroup.setAvatar(avatar);
            }
            return userGroup;
        }
        return null;
    }

    private MediaFile generateAvatarEntity(@NonNull JsonMediaFile jsonMediaFile) {
        var avatar = MediaFile
                .builder()
                .fileType(jsonMediaFile.getFileType())
                .mimeType(jsonMediaFile.getMimeType())
                .path(jsonMediaFile.getPath())
                .storageType(StorageType.MINIO)
                .size(jsonMediaFile.getSize())
                .state(MediaFileState.CREATED)
                .build();
        if (!CollectionUtils.isEmpty(jsonMediaFile.getSizedImages())) {
            var sizedImages = jsonMediaFile.getSizedImages();
            Set<SizedImage> sizedImageSet = new HashSet<>();
            for (List<JsonSizedImage> sizedImageList : sizedImages.values()) {
                sizedImageSet.addAll(sizedImageList.stream()
                        .filter(sizedImage -> sizedImage.getPath() != null)
                        .map(sizedImage -> SizedImage
                                .builder()
                                .mimeType(sizedImage.getMimeType())
                                .size(sizedImage.getSize())
                                .path(sizedImage.getPath())
                                .imageSizeType(sizedImage.getImageSizeType())
                                .storageType(StorageType.MINIO)
                                .createdAt(sizedImage.getCreatedAt())
                                .build())
                        .toList());
            }
            avatar.setSizedImages(sizedImageSet);
        }
        return avatar;
    }
}
