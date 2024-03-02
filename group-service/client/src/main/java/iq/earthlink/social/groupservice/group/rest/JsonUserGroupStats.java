package iq.earthlink.social.groupservice.group.rest;

import lombok.Data;

@Data
public class JsonUserGroupStats {
    private Long id;
    private Long membersCount;
    private Long pendingPostsCount;
    private Long pendingJoinRequests;
    private Long publishedPostsCount;
    private Long score;
}
