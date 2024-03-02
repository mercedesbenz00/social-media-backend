package iq.earthlink.social.classes.enumeration;

import com.google.common.collect.ImmutableList;
import lombok.Getter;

import java.util.List;

public enum NotificationType {
    POST_CREATED("post.created", new String[] {}),
    POST_STATE_CHANGED("post.state.is.changed", new String[] {Constants.OLD_STATE, Constants.NEW_STATE}),
    POST_COMMENTED("post.commented", new String[] {Constants.AUTHOR_NAME, Constants.GROUP_NAME, Constants.COMMENT_TEXT}),
    POST_COMMENTED_EXT("post.commented.ext", new String[] {Constants.AUTHOR_NAME, Constants.GROUP_NAME, Constants.COMMENT_TEXT}),
    COMMENT_REPLIED("post.comment.replied", new String[] {Constants.AUTHOR_NAME, Constants.GROUP_NAME, Constants.COMMENT_TEXT}),
    PERSON_IS_MENTIONED_IN_POST("mentioned.in.post", new String[] {Constants.AUTHOR_NAME}),
    PERSON_IS_MENTIONED_IN_COMMENT("mentioned.in.comment", new String[] {Constants.AUTHOR_NAME}),
    POST_UP_VOTED("post.voted.up", new String[] {Constants.AUTHOR_NAME}),
    POST_DOWN_VOTED("post.voted.down", new String[] {Constants.AUTHOR_NAME}),
    COMMENT_UP_VOTED("comment.voted.up", new String[] {Constants.AUTHOR_NAME}),
    COMMENT_DOWN_VOTED("comment.voted.down", new String[] {Constants.AUTHOR_NAME}),
    STORY_CREATED("story.created", new String[] {Constants.AUTHOR_NAME}),
    USER_INVITED_TO_GROUP("group.invited", new String[] {Constants.AUTHOR_NAME, Constants.GROUP_NAME}),
    GROUP_JOIN_REQUESTED("group.join.requested", new String[] {Constants.AUTHOR_NAME, Constants.GROUP_NAME}),
    GROUP_JOIN_REQUEST_APPROVED("group.join.request.approved", new String[] {Constants.GROUP_NAME}),
    GROUP_JOIN_REQUEST_REJECTED("group.join.request.rejected", new String[] {Constants.GROUP_NAME}),
    GROUP_JOIN_REQUEST_COMPLETED("group.join.request.completed", new String[] {Constants.AUTHOR_NAME, Constants.GROUP_NAME}),
    POST_DELETED_BY_MODERATOR("offensive.post.deleted", new String[] {Constants.GROUP_NAME, Constants.POST_TEXT}),
    COMMENT_DELETED_BY_MODERATOR("offensive.comment.deleted", new String[] {Constants.GROUP_NAME, Constants.COMMENT_TEXT}),

    POST_APPROVED_BY_GROUP_ADMIN("post.approved", new String[] {Constants.GROUP_NAME, Constants.POST_TEXT}),
    POST_REJECTED_BY_GROUP_ADMIN("post.rejected", new String[] {Constants.GROUP_NAME, Constants.POST_TEXT}),
    USER_FOLLOWED("user.followed", new String[] {Constants.AUTHOR_NAME}),
    POST_VOTE_ADDED("post.vote.added", new String[] {Constants.AUTHOR_NAME, Constants.GROUP_NAME}),
    POST_PUBLISHED_TO_GROUP("post.published.to.group", new String[] {Constants.GROUP_NAME, Constants.POST_TEXT}),
    POST_PUBLISHED_TO_GROUP_EXT("post.published.to.group.ext", new String[] {Constants.AUTHOR_NAME, Constants.GROUP_NAME, Constants.POST_TEXT}),
    COMMENT_VOTE_ADDED("comment.vote.added", new String[] {Constants.AUTHOR_NAME, Constants.COMMENT_TEXT, Constants.GROUP_NAME});

    @Getter
    private final String messageId;
    
    @Getter
    private final List<String> messagePlaceholders;

    NotificationType(String messageId, String[] messagePlaceholders) {
        this.messageId = messageId;
        this.messagePlaceholders = ImmutableList.copyOf(messagePlaceholders);
    }

    private static class Constants {
        public static final String AUTHOR_NAME = "authorName";
        public static final String GROUP_NAME = "groupName";
        public static final String COMMENT_TEXT = "commentText";
        public static final String POST_TEXT = "postText";
        public static final String OLD_STATE = "oldState";
        public static final String NEW_STATE = "newState";
    }
}
