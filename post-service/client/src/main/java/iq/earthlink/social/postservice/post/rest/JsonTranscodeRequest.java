package iq.earthlink.social.postservice.post.rest;

import lombok.Data;

@Data
public class JsonTranscodeRequest {
    private String id;
    private Long postId;
    private String objectName;
    private String bucket;
    private String callbackUrl;
}
