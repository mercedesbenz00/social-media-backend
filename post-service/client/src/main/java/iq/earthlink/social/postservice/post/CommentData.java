package iq.earthlink.social.postservice.post;

import java.util.List;
import java.util.UUID;

public interface CommentData {

  UUID getPostUuid();

  String getContent();

  List<Long> getMentionedPersonIds();

  boolean isAllowEmptyContent();
}
