package iq.earthlink.social.postservice.post.rest;

import iq.earthlink.social.classes.enumeration.TimeInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostStats {

  private long allPostsCount;
  private long newPostsCount;
  private List<CreatedPosts> createdPosts;
  private TimeInterval timeInterval;
  private Timestamp fromDate;

}

