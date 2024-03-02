package iq.earthlink.social.commentservice.dto;

import java.util.List;
import java.util.UUID;

public interface CommentData {
    String getContent();
    Long getAuthorId();
    UUID getObjectId();
    List<Long> getMentionedPersonIds();
}
