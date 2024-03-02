package iq.earthlink.social.classes.data.dto;

import iq.earthlink.social.classes.enumeration.PostState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
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
    private PostState state;
    private PostResponse repostedFrom;
    private boolean commentsAllowed;
    private Map<String, String> linkMeta;
}
