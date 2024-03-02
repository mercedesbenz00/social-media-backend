package iq.earthlink.social.postservice.post;

import iq.earthlink.social.postservice.post.rest.JsonReason;

public interface CommentComplaintData {

  JsonReason getReason();
  String getReasonOther();
}
