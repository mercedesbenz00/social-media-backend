package iq.earthlink.social.postservice.post.comment.complaint;

public class CommentComplaintNotFoundException extends RuntimeException {

  public CommentComplaintNotFoundException(String message) {
    super(message);
  }

  public CommentComplaintNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
