package iq.earthlink.social.groupservice.controller.rest.v1.category;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.groupservice.category.CategoryManager;
import iq.earthlink.social.groupservice.category.CategoryMediaService;
import iq.earthlink.social.groupservice.util.RoleUtil;
import iq.earthlink.social.security.DefaultSecurityProvider;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static iq.earthlink.social.common.file.rest.DownloadUtils.fileResponse;

@Api(tags = "Category Media Api", value = "CategoryMediaApi")
@RestController
@RequestMapping("/api/v1/categories/{categoryId}")
public class CategoryMediaApi {

    private final CategoryMediaService mediaService;
    private final CategoryManager categoryManager;
    private final DefaultSecurityProvider securityProvider;
    private final RoleUtil roleUtil;

    public CategoryMediaApi(CategoryMediaService mediaService, CategoryManager categoryManager, DefaultSecurityProvider securityProvider, RoleUtil roleUtil) {
        this.mediaService = mediaService;
        this.categoryManager = categoryManager;
        this.securityProvider = securityProvider;
        this.roleUtil = roleUtil;
    }

    @ApiOperation(value = "Saves avatar for the category")
    @PostMapping(value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MediaFile saveAvatar(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long categoryId,
            MultipartFile file
    ) {
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        return mediaService.uploadAvatar(categoryManager.getCategory(categoryId), file, isAdmin);
    }

    @ApiOperation(value = "Returns the avatar image file for the category", response = InputStream.class)
    @GetMapping(value = "/avatar", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadAvatar(
            @PathVariable Long categoryId
    ) {
        MediaFile avatar = mediaService.findAvatar(categoryId)
                .orElseThrow(() -> new NotFoundException("error.not.found.avatar.for.category", categoryId));

        return fileResponse(avatar, mediaService.downloadAvatar(avatar));
    }

    @ApiOperation(value = "Removes the avatar for the category")
    @DeleteMapping(value = "/avatar")
    public void removeAvatar(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long categoryId
    ) {
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        mediaService.removeAvatar(categoryManager.getCategory(categoryId), isAdmin);
    }

    @ApiOperation(value = "Saves cover for the category")
    @PostMapping(value = "/cover",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MediaFile saveCover(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long categoryId,
            MultipartFile file
    ) {
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        return mediaService.uploadCover(categoryManager.getCategory(categoryId), file, isAdmin);
    }

    @ApiOperation(value = "Returns the cover image file for the category", response = InputStream.class)
    @GetMapping(value = "/cover", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadCover(
            @PathVariable Long categoryId
    ) {

        MediaFile cover = mediaService.findCover(categoryId)
                .orElseThrow(() -> new NotFoundException("error.not.found.cover.for.category", categoryId));

        return fileResponse(cover, mediaService.downloadCover(cover));
    }

    @ApiOperation(value = "Removes the cover for the category")
    @DeleteMapping(value = "/cover")
    public void removeCover(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long categoryId
    ) {
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        mediaService.removeCover(categoryManager.getCategory(categoryId), isAdmin);
    }
}
