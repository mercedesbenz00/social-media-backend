package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.PersonMediaService;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.security.SecurityProvider;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static iq.earthlink.social.common.file.rest.DownloadUtils.fileResponse;

@Api(tags = "Person Media Api", value = "PersonMediaApi")
@RestController
@RequestMapping("/api/v1/persons/{personId}")
public class PersonMediaApi {

    private final PersonMediaService mediaService;
    private final PersonManager personManager;
    private final SecurityProvider securityProvider;

    public PersonMediaApi(PersonMediaService mediaService,
                          PersonManager personManager, SecurityProvider securityProvider) {
        this.mediaService = mediaService;
        this.personManager = personManager;
        this.securityProvider = securityProvider;
    }

    @ApiOperation(value = "Saves avatar for the person")
    @PostMapping(value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MediaFile saveAvatar(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long personId,
            MultipartFile file
    ) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);

        return mediaService.uploadAvatar(personManager.getPersonByIdInternal(personId), file, currentUser);
    }

    @ApiOperation(value = "Returns the avatar image file for the person", response = InputStream.class)
    @GetMapping(value = "/avatar", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadAvatar(
            @PathVariable Long personId
    ) {
        MediaFile avatar = mediaService.findAvatar(personId)
                .orElseThrow(() -> new NotFoundException("error.person.avatar.not.found", personId));

        return fileResponse(avatar, mediaService.downloadAvatar(avatar));
    }

    @ApiOperation(value = "Removes avatar for the person")
    @DeleteMapping(value = "/avatar")
    public void removeAvatar(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long personId
    ) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);

        mediaService.removeAvatar(personManager.getPersonByIdInternal(personId), currentUser);
    }

    @ApiOperation(value = "Saves cover for the person")
    @PostMapping(value = "/cover",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MediaFile saveCover(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long personId,
            MultipartFile file
    ) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);

        return mediaService.uploadCover(personManager.getPersonByIdInternal(personId), file, currentUser);
    }

    @ApiOperation(value = "Returns the avatar image file for the person", response = InputStream.class)
    @GetMapping(value = "/cover", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadCover(
            @PathVariable Long personId
    ) {

        MediaFile cover = mediaService.findCover(personId)
                .orElseThrow(() -> new NotFoundException("error.person.cover.not.found", personId));

        return fileResponse(cover, mediaService.downloadCover(cover));
    }

    @ApiOperation(value = "Removes cover for the person")
    @DeleteMapping(value = "/cover")
    public void removeCover(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long personId
    ) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);

        mediaService.removeCover(personManager.getPersonByIdInternal(personId), currentUser);
    }
}
