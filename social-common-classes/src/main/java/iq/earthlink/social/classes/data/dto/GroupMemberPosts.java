package iq.earthlink.social.classes.data.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class GroupMemberPosts implements Serializable {
    Long userGroupId;
    Long authorId;
    Long postsCount;

    public GroupMemberPosts( Long userGroupId, Long authorId, Long postsCount) {
        this.userGroupId = userGroupId;
        this.authorId = authorId;
        this.postsCount = postsCount;
    }
}
