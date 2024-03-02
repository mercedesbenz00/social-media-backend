package iq.earthlink.social.postservice.post.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatedPosts {

  private String date;
  private long createdPostsCount;

}

