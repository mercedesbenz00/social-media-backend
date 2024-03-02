package iq.earthlink.social.postservice.post.dto;

import iq.earthlink.social.classes.data.dto.*;
import iq.earthlink.social.postservice.post.rest.JsonComment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostPublicDTO  {
    private Long id;
    private String postUuid;
    private String content;
    private Date publishedAt;
    private Date createdAt;
    private String postType;
    private GroupDetails group;
    private AuthorDetails author;
    private List<JsonMediaFile> files;
    private PostDetailStatistics stats;
    private PostResponse repostedFrom;
    private Map<String, String> linkMeta;
    private List<JsonComment> comments;
}
