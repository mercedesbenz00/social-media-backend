package iq.earthlink.social.postservice.post.vote;

import lombok.Data;

@Data
public class VoteCount {
  private final long id; // for ex. postId or commentId
  private final long upvotesTotal;
  private final long downvotesTotal;
  private int voteValue = 0;
}
