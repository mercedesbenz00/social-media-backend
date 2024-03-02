package iq.earthlink.social.postservice.controller.v1.pub;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.postservice.post.BFFPostManager;
import iq.earthlink.social.postservice.post.dto.PostPublicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Api(value = "PublicPostApi", tags = "Public Post Api")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/public/v1/posts", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicPostApi {

    private final BFFPostManager postManager;

    @ApiOperation("Returns the post found by provided id")
    @GetMapping("/{postUuid}")
    public PostPublicDTO getPostByUuid(@PathVariable UUID postUuid) {
        return postManager.getPostByUuid(postUuid);
    }

}
