package iq.earthlink.social.postservice.post;

public class PostNotFoundException extends RuntimeException {

  public PostNotFoundException(String message) {
    super(message);
  }

  public PostNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
