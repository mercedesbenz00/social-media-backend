package iq.earthlink.social.postservice.post.collection;

public class PostCollectionNotFoundException extends RuntimeException {

  public PostCollectionNotFoundException(String message) {
    super(message);
  }

  public PostCollectionNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
