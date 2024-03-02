package iq.earthlink.social.postservice.post;

import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.postservice.post.rest.LinkMeta;

import java.util.List;

public interface UpdatePostData {

  String getContent();

  Boolean getCommentsAllowed();

  PostState getState();

  Boolean getShouldPin();

  List<Long> getMentionedPersonIds();

  LinkMeta getLinkMeta();
}
