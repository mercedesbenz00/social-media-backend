package iq.earthlink.social.shortvideoregistryservice.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import iq.earthlink.social.security.DefaultSecurityProvider;
import iq.earthlink.social.shortvideoregistryservice.dto.*;
import iq.earthlink.social.shortvideoregistryservice.service.ShortVideoRegistryService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@Profile("api")
@RestController
@RequestMapping(value = "/api/v1/short-video", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class ShortVideoRegistryAPI {


    private final ShortVideoRegistryService shortVideoRegistryService;
    private final DefaultSecurityProvider securityProvider;

    public ShortVideoRegistryAPI(ShortVideoRegistryService shortVideoRegistryService, DefaultSecurityProvider securityProvider) {
        this.shortVideoRegistryService = shortVideoRegistryService;
        this.securityProvider = securityProvider;
    }

    /**
     * GET /{videoId} : Retrieves a short video object by ID
     *
     * @param videoId Short Video ID (required)
     * @return A short video (status code 200)
     * or Unable to find resource (status code 404)
     * or Error accessing the service consistent with http status code (status code 200)
     */
    @Operation(
            operationId = "findShortVideoById",
            summary = "Retrieves a short video object by ID",
            tags = {"Short Video Registry"}
    )
    @GetMapping(
            value = "/{videoId}",
            produces = {"application/json"}
    )
    ResponseEntity<ShortVideoDTO> findShortVideoById(
            @Parameter(name = "videoId", description = "Short Video ID", required = true, schema = @Schema()) @PathVariable("videoId") UUID videoId
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(shortVideoRegistryService.findShortVideoById(videoId));
    }


    @Operation(
            operationId = "findShortVideosByAuthor",
            summary = "Retrieves short video objects by author",
            tags = {"Short Video Registry"}
    )
    @GetMapping(
            value = "/by-author",
            produces = {"application/json"}
    )
    ResponseEntity<CassandraPageDTO<ShortVideoDTO>> findShortVideosByAuthor(
            @Parameter(name = "authorId", description = "Short Video Author ID", schema = @Schema()) @Valid @RequestParam(value = "authorId") Long authorId,
            @Parameter(name = "fromDate", example = "2020-01-01", description = "Date to find short videos from, it should not be less than mentioned year", schema = @Schema()) @Valid @RequestParam(value = "fromDate", required = false) String fromDate,
            PageInfo pageInfo
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(shortVideoRegistryService.findShortVideosByAuthor(authorId, fromDate, pageInfo));
    }

    @Operation(
            operationId = "findShortVideosByCategories",
            summary = "Retrieves short video objects by categories",
            tags = {"Short Video Registry"}
    )
    @GetMapping(
            value = "/by-categories",
            produces = {"application/json"}
    )
    ResponseEntity<CassandraPageDTO<ShortVideoDTO>> findShortVideosByCategories(
            @Parameter(name = "categoryIds", description = "Short Video Category ID", schema = @Schema()) @Valid @RequestParam(value = "categoryIds") List<UUID> categoryIds,
            @Parameter(name = "fromDate", example = "2020-01-01", description = "Date to find short videos from", schema = @Schema()) @Valid @RequestParam(value = "fromDate", required = false) String fromDate,
            PageInfo pageInfo
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(shortVideoRegistryService.findShortVideosByCategory(categoryIds, fromDate, pageInfo));
    }

    @Operation(
            operationId = "findShortVideosOfFriends",
            summary = "Retrieves short video objects of friends",
            tags = {"Short Video Registry"}
    )
    @GetMapping(
            value = "/friends",
            produces = {"application/json"}
    )
    ResponseEntity<CassandraPageDTO<ShortVideoDTO>> findShortVideosOfFriends(
            @RequestHeader("Authorization") String authorizationHeader,
            @Parameter(name = "friendUserName", description = "Friend username", schema = @Schema())
            @Valid @RequestParam(value = "friendUserName") String friendUserName,
            @Parameter(name = "fromDate", example = "2020-01-01", description = "Date to find short videos from",
                    schema = @Schema())
            @Valid @RequestParam(value = "fromDate", required = false) String fromDate,
            PageInfo pageInfo
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return ResponseEntity.status(HttpStatus.OK).body(shortVideoRegistryService.findShortVideosOfFriends(personId, friendUserName, fromDate, pageInfo));
    }

    /**
     * GET /configuration : Get short video configuration
     *
     * @return Successfully get objects (status code 200)
     * or Error accessing the service consistent with http status code (status code 200)
     */
    @Operation(
            operationId = "getShortVideoConfiguration",
            summary = "Get short video configuration",
            tags = {"Short Video Registry"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully get objects", content = {
                            @Content(mediaType = "application/json")
                    }),
                    @ApiResponse(responseCode = "200", description = "Error accessing the service consistent with http status code", content = {
                            @Content(mediaType = "application/json")
                    })
            }
    )
    @GetMapping(
            value = "/configuration",
            produces = {"application/json"}
    )
    ResponseEntity<ShortVideoConfigurationDTO> getShortVideoConfiguration(

    ) {
        return ResponseEntity.status(HttpStatus.OK).body(shortVideoRegistryService.getShortVideoConfiguration());
    }


    /**
     * POST /configuration : Set short video configuration
     *
     * @param shortVideoConfigurationDTO Short video configuration object (optional)
     * @return Ok (status code 200)
     * or Error accessing the service consistent with http status code (status code 200)
     */
    @Operation(
            operationId = "setShortVideoConfiguration",
            summary = "Set short video configuration",
            tags = {"Short Video Registry"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ok"),
                    @ApiResponse(responseCode = "200", description = "Error accessing the service consistent with http status code", content = {
                            @Content(mediaType = "application/json")
                    })
            }
    )
    @PostMapping(
            value = "/configuration",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    ResponseEntity<Void> setShortVideoConfiguration(
            @Parameter(name = "ShortVideoConfigurationDTO", description = "Short video configuration object", schema = @Schema()) @Valid @RequestBody(required = false) ShortVideoConfigurationDTO shortVideoConfigurationDTO
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(shortVideoRegistryService.setShortVideoConfiguration(shortVideoConfigurationDTO));
    }


    /**
     * PUT /{videoId} : Updates a short videos
     *
     * @param videoId       Short Video ID (required)
     * @param updateShortVideoDTO Fields to be updated (required)
     * @return Short video successfully updated (status code 200)
     * or Unable to find resource (status code 404)
     * or Error accessing the service consistent with http status code (status code 200)
     */
    @Operation(
            operationId = "updateShortVideo",
            summary = "Updates a short videos",
            tags = {"Short Video Registry"}
    )
    @PutMapping(
            value = "/{videoId}",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    ResponseEntity<ShortVideoDTO> updateShortVideo(
            @Parameter(name = "videoId", description = "Short Video ID", required = true, schema = @Schema()) @PathVariable("videoId") UUID videoId,
            @Parameter(name = "UpdateShortVideoRequestDTO", description = "Fields to be updated", required = true, schema = @Schema()) @Valid @RequestBody UpdateShortVideoRequestDTO updateShortVideoDTO
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(shortVideoRegistryService.updateShortVideo(videoId, updateShortVideoDTO));
    }

    /**
     * PUT /{videoId}/votes/{voteType} : Adds a vote to the short video
     *
     * @param videoId       Short Video ID (required)
     * @param voteType      Vote type: 1 - like, 2 - dislike (required)
     * @return Updated short video statistics (status code 200)
     * or Unable to find resource (status code 404)
     */
    @ApiOperation("Adds person vote to the short video. Returns updated short video statistics")
    @Operation(
            operationId = "addShortVideoVote",
            summary = " @ApiOperation(\"Adds person vote to the short video. Returns updated short video statistics.\")",
            tags = {"Short Video Registry"}
    )
    @PutMapping("/{videoId}/votes/{voteType}")
    ResponseEntity<ShortVideoStatsDTO> addShortVideoVote(
            @RequestHeader("Authorization") String authorizationHeader,
            @Parameter(name = "videoId", description = "Short Video ID", required = true, schema = @Schema())
            @PathVariable("videoId") UUID videoId,
            @Parameter(name = "voteType", description = "Vote type", required = true, schema = @Schema())
            @PathVariable("voteType") Integer voteType
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return ResponseEntity.status(HttpStatus.OK).body(shortVideoRegistryService.addShortVideoVote(personId, videoId, voteType));
    }

    /**
     * DELETE /{videoId}/votes: Deletes a person vote to the short video
     *
     * @param videoId Short Video ID (required)
     * @return Updated short video statistics (status code 200)
     * or Unable to find resource (status code 404)
     */
    @ApiOperation("Removes vote for a short video that has already been set. Returns updated short video statistics")
    @DeleteMapping("/{videoId}/votes")
    public ResponseEntity<ShortVideoStatsDTO> deleteShortVideoVote(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Id of the short video")
            @PathVariable("videoId") UUID videoId
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return ResponseEntity.status(HttpStatus.OK).body(shortVideoRegistryService.deleteShortVideoVote(personId, videoId));
    }

    /**
     * PUT /{videoId}/comments/{commentsOffset}: Updates short video comment counter
     *
     * @param videoId        Short Video ID (required)
     * @param commentDeleted Indicates if comment was deleted. Optional, false by default
     * @return Updated short video statistics (status code 200)
     * or Unable to find resource (status code 404)
     */
    @ApiOperation("Updates short video comment counter. Returns updated short video statistics")
    @Operation(
            operationId = "update",
            summary = " @ApiOperation(\"Updates short video comment counter. Returns updated short video statistics.\")",
            tags = {"Short Video Registry"}
    )
    @PutMapping("/{videoId}/comments")
    ResponseEntity<ShortVideoStatsDTO> updateCommentStats(
            @RequestHeader("Authorization") String authorizationHeader,
            @Parameter(name = "videoId", description = "Short Video ID", required = true, schema = @Schema())
            @PathVariable("videoId") UUID videoId,
            @RequestParam(value = "commentDeleted", required = false, defaultValue = "false")  boolean commentDeleted
    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        return ResponseEntity.status(HttpStatus.OK).body(shortVideoRegistryService.updateCommentStats(personId, videoId, commentDeleted));
    }
}
