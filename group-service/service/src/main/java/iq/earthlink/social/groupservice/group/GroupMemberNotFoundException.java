package iq.earthlink.social.groupservice.group;

public class GroupMemberNotFoundException extends RuntimeException {

  public GroupMemberNotFoundException(String message) {
    super(message);
  }

  public GroupMemberNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
