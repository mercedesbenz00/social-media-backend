package iq.earthlink.social.postservice.post.complaint;

public class PostComplaintNotFoundException extends RuntimeException {

  public PostComplaintNotFoundException(String message) {
    super(message);
  }

  public PostComplaintNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
