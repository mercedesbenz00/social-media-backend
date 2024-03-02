package iq.earthlink.social.common.audit;

import java.util.EnumSet;
import java.util.Set;

public enum EventAction {
    MUTE(EventCategory.USER, "userId"),
    UNMUTE(EventCategory.USER, "userId"),
    BAN(EventCategory.USER, "userId"),
    BLOCK(EventCategory.USER, "userId"),

    CREATE_GROUP(EventCategory.GROUP, "groupId"),
    UPDATE_GROUP_RULES(EventCategory.GROUP, "groupId"),
    CHANGE_GROUP_SETTINGS(EventCategory.GROUP, "groupId"),
    REMOVE_USER_FROM_GROUP(EventCategory.GROUP, "groupId"),
    CREATE_CATEGORY(EventCategory.GROUP, "categoryId"),
    ASSIGN_GROUP_ADMIN(EventCategory.GROUP, "groupPermissionId"),
    INVITE_TO_GROUP(EventCategory.GROUP, "groupId"),

    APPROVE_POST(EventCategory.POST, "postId"),
    REJECT_POST(EventCategory.POST, "postId"),
    DELETE_POST_COMMENT(EventCategory.POST, "commentId"),
    EDIT_POST_COMMENT(EventCategory.POST, "commentId");

    public static Set<EventAction> userActions = EnumSet.of(MUTE, UNMUTE, BAN, BLOCK);
    public static Set<EventAction> groupActions = EnumSet.of(CREATE_GROUP, UPDATE_GROUP_RULES, CHANGE_GROUP_SETTINGS, REMOVE_USER_FROM_GROUP);
    public static Set<EventAction> postActions = EnumSet.of(APPROVE_POST, REJECT_POST, DELETE_POST_COMMENT, EDIT_POST_COMMENT);

    private final EventCategory category;
    private final String referenceName;

    EventAction(EventCategory category, String referenceName) {
        this.category = category;
        this.referenceName = referenceName;
    }

    public EventCategory getCategory() {
        return category;
    }

    public String getReferenceName() {
        return referenceName;
    }
}
