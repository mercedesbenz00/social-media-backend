package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.postservice.post.linkpreview.LinkPreviewResponse;
import iq.earthlink.social.postservice.post.linkpreview.LinkPreviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "LinkPreviewApi", tags = "Link Preview Api")
@RestController
@RequestMapping("/api/v1/link-preview")
public class LinkPreviewApi {

    private final LinkPreviewService linkPreviewService;

    public LinkPreviewApi(LinkPreviewService linkPreviewService) {
        this.linkPreviewService = linkPreviewService;
    }

    @ApiOperation("Get link preview with title, description and image")
    @GetMapping
    public ResponseEntity<LinkPreviewResponse> getLinkPreview(
            @ApiParam("Link")
            @RequestParam String link) {
        LinkPreviewResponse linkPreviewResponse = linkPreviewService.getLinkPreview(link);
        return ResponseEntity.ok(linkPreviewResponse);
    }

}
