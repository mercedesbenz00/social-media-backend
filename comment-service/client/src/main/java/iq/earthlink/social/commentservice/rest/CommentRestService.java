package iq.earthlink.social.commentservice.rest;

import iq.earthlink.social.commentservice.dto.JsonComment;
import iq.earthlink.social.commentservice.dto.JsonCommentData;
import iq.earthlink.social.common.rest.RestPageImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "comment-service", url = "${comment.service.url}")
@Component
public interface CommentRestService {

    @PostMapping(value = "/api/v1/comment")
    JsonComment createComment(
            @RequestHeader("Authorization") String authorizationHeader,
            JsonCommentData commentData);

    @PutMapping(value = "/api/v1/comment/{commentId}")
    JsonComment updateComment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable(value = "commentId") Long commentId,
            JsonCommentData commentData);

    @DeleteMapping(value = "/api/v1/comment/{commentId}")
    JsonComment removeComment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable(value = "commentId") Long commentId,
            @RequestParam(name = "objectId") UUID objectId);

    @GetMapping(value = "/api/v1/comment")
    RestPageImpl<JsonComment> findComments(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(name = "objectId") UUID objectId,
            @RequestParam(name = "showAll", required = false) Boolean showAll,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size);

    @GetMapping(value = "/api/v1/comment/{commentId}")
    JsonComment getComment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable(value = "commentId") Long commentId,
            @RequestParam(name = "objectId") UUID objectId);

    @PostMapping(value = "/api/v1/comment/{commentId}/reply")
    JsonComment reply(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable(value = "commentId") Long commentId,
            JsonCommentData commentData);

    @GetMapping(value = "/api/v1/comment/{commentId}/reply")
    RestPageImpl<JsonComment> getCommentReplies(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable(value = "commentId") Long commentId,
            @RequestParam(name = "objectId") UUID objectId,
            @RequestParam(name = "showAll", required = false) Boolean showAll,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size);
}
