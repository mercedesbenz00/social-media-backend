package iq.earthlink.social.postservice.post.linkpreview;

import org.springframework.web.bind.annotation.RequestParam;

public interface LinkPreviewService {
    LinkPreviewResponse getLinkPreview(@RequestParam String url);
}
