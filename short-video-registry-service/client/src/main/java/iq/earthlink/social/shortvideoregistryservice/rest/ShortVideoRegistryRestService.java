package iq.earthlink.social.shortvideoregistryservice.rest;

import iq.earthlink.social.shortvideoregistryservice.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "short-video-registry-service", url = "${short-video-registry.service.url}")
@Component
public interface ShortVideoRegistryRestService {

    @GetMapping(value = "/api/v1/short-video/configuration")
    ShortVideoConfigurationDTO getShortVideoConfiguration(
            @RequestHeader(value = "Authorization") String authorizationHeader);

    @PostMapping(value = "/api/v1/short-video/configuration")
    void setShortVideoConfiguration(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            ShortVideoConfigurationDTO shortVideoConfigurationDTO);

    @GetMapping(value = "/api/v1/short-video/{videoId}")
    ShortVideoDTO findShortVideoById(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @PathVariable(value = "videoId") UUID videoId);

    @GetMapping(value = "/api/v1/short-video/by-author")
    CassandraPageDTO<ShortVideoDTO> findShortVideosByAuthor(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestParam(value = "authorId", required = false) Long authorId,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "pagingState", required = false) String page);

    @GetMapping(value = "/api/v1/short-video/by-categories")
    CassandraPageDTO<ShortVideoDTO> findShortVideosByCategories(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestParam(value = "categoryIds", required = false) List<String> categoryIds,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "pagingState", required = false) String page);

    @PutMapping(value = "/api/v1/short-video/{videoId}")
    ShortVideoDTO updateShortVideo(@RequestHeader(value = "Authorization") String authorizationHeader,
                                   @PathVariable(value = "videoId") UUID videoId,
                                   @RequestBody UpdateShortVideoRequestDTO shortVideoDTO);

    @GetMapping(value = "/api/v1/short-video/friends")
    CassandraPageDTO<ShortVideoDTO> findShortVideosOfFriends(
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestParam(value = "userId") Long userId,
            @RequestParam(value = "friendUserName") String friendUserName,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "pagingState", required = false) String page);

    @PutMapping(value = "/api/v1/short-video/{videoId}/votes/{voteType}")
    ShortVideoStatsDTO addShortVideoVote(@RequestHeader(value = "Authorization") String authorizationHeader,
                                        @PathVariable(value = "videoId") UUID videoId,
                                        @PathVariable(value = "voteType") Integer voteType);

    @DeleteMapping(value = "/api/v1/short-video/{videoId}/votes")
    ShortVideoStatsDTO deleteShortVideoVote(@RequestHeader(value = "Authorization") String authorizationHeader,
                              @PathVariable(value = "videoId") UUID videoId);

    @PutMapping(value = "/api/v1/short-video/{videoId}/comments")
    ShortVideoStatsDTO updateCommentStats(@RequestHeader(value = "Authorization") String authorizationHeader,
                                          @PathVariable(value = "videoId") UUID videoId,
                                          @RequestParam(value = "commentDeleted", required = false)  boolean commentDeleted);
}
