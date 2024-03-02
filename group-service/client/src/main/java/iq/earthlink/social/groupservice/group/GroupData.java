package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.groupservice.group.rest.enumeration.*;

import java.util.Set;

public interface GroupData {

    String getName();

    String getDescription();

    String getRules();

    Set<Long> getCategories();

    Set<Long> getTags();

    AccessType getAccessType();

    PostingPermission getPostingPermission();

    InvitePermission getInvitePermission();

    ApprovalState getState();

    GroupVisibility getVisibility();
}
