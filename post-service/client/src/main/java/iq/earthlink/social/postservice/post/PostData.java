package iq.earthlink.social.postservice.post;

import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.postservice.post.rest.LinkMeta;

import java.util.List;

public interface PostData {

  String getContent();

  Long getUserGroupId();

  Boolean getCommentsAllowed();

  PostState getState();

  Boolean getShouldPin();

  Long getRepostedFromId();

  List<Long> getMentionedPersonIds();

  LinkMeta getLinkMeta();
}
