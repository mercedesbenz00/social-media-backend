package iq.earthlink.social.groupservice.controller.rest.v1.group;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.groupservice.group.GroupManagerUtils;
import iq.earthlink.social.groupservice.group.GroupMediaService;
import iq.earthlink.social.groupservice.util.RoleUtil;
import iq.earthlink.social.security.DefaultSecurityProvider;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static iq.earthlink.social.common.file.rest.DownloadUtils.fileResponse;

@Api(tags = "Group Media Api", value = "GroupMediaApi")
@RestController
@RequestMapping("/api/v1/groups/{groupId}")
public class GroupMediaApi {

    private final GroupManagerUtils groupManagerUtils;
    private final GroupMediaService mediaService;
    private final DefaultSecurityProvider securityProvider;
    private final RoleUtil roleUtil;

    public GroupMediaApi(GroupManagerUtils groupManagerUtils,
                         GroupMediaService mediaService,
                         DefaultSecurityProvider securityProvider,
                         RoleUtil roleUtil) {
        this.groupManagerUtils = groupManagerUtils;
        this.mediaService = mediaService;
        this.securityProvider = securityProvider;
        this.roleUtil = roleUtil;
    }

    @ApiOperation(value = "Saves new avatar for the group")
    @PostMapping(value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MediaFile saveAvatar(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            MultipartFile file
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        return mediaService.uploadAvatar(groupManagerUtils.getGroup(groupId), file, personId, isAdmin);
    }

    @ApiOperation(value = "Returns the avatar image file for the group", response = InputStream.class)
    @GetMapping(value = "/avatar", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadAvatar(
            @PathVariable Long groupId
    ) {
        MediaFile avatar = mediaService.findAvatar(groupId)
                .orElseThrow(() -> new NotFoundException("error.not.found.avatar.for.group", groupId));

        return fileResponse(avatar, mediaService.downloadAvatar(avatar));
    }

    @ApiOperation(value = "Removes the avatar for the group")
    @DeleteMapping(value = "/avatar")
    public void removeAvatar(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        mediaService.removeAvatar(groupManagerUtils.getGroup(groupId), personId, isAdmin);
    }

    @ApiOperation(value = "Saves new cover for the group")
    @PostMapping(value = "/cover",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MediaFile saveCover(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            MultipartFile file
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        return mediaService.uploadCover(groupManagerUtils.getGroup(groupId), file, personId, isAdmin);
    }

    @ApiOperation(value = "Returns the cover image file for the group", response = InputStream.class)
    @GetMapping(value = "/cover", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadCover(
            @PathVariable Long groupId
    ) {

        MediaFile cover = mediaService.findCover(groupId)
                .orElseThrow(() -> new NotFoundException("error.not.found.cover.for.group", groupId));

        return fileResponse(cover, mediaService.downloadCover(cover));
    }

    @ApiOperation(value = "Removes the cover for the group")
    @DeleteMapping(value = "/cover")
    public void removeCover(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        mediaService.removeCover(groupManagerUtils.getGroup(groupId), personId, isAdmin);
    }
}
