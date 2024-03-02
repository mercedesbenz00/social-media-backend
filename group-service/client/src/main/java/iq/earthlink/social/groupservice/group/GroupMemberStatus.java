package iq.earthlink.social.groupservice.group;

import java.util.stream.Stream;

public enum GroupMemberStatus {
  USER, MODERATOR, ADMIN, NOT_MEMBER;

  public boolean isOneOf(GroupMemberStatus... statuses) {
    return Stream.of(statuses).anyMatch(s -> s == this);
  }
}
