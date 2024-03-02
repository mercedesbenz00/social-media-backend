package iq.earthlink.social.postservice.story;

public class StoryNotFoundException extends StoryManagerException {

  public StoryNotFoundException(String message) {
    super(message);
  }

  public StoryNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
