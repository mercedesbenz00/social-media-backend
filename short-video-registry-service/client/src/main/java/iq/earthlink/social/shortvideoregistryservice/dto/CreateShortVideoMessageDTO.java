package iq.earthlink.social.shortvideoregistryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateShortVideoMessageDTO {
    private UUID id;
    private String bucket;
    private String title;
    private Long authorId;
    private Set<ShortVideoCategoryDTO> categories = new HashSet<>();
    private Set<ShortVideoFriendDTO> friends = new HashSet<>();
    private String url;
    private String thumbnailUrl;
    private Long videoDuration;
    private Long likesCount;
    private Long commentsCount;
    private Long playCount;
}
