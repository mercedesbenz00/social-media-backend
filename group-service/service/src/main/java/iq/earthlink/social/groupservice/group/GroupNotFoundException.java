package iq.earthlink.social.groupservice.group;

public class GroupNotFoundException extends RuntimeException {

  public GroupNotFoundException(String message) {
    super(message);
  }

  public GroupNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
