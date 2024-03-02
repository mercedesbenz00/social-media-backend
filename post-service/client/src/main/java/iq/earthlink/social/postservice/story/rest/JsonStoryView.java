package iq.earthlink.social.postservice.story.rest;

import lombok.Data;

import java.util.Date;

@Data
public class JsonStoryView {

  private Long id;
  private JsonStory story;
  private Long viewerId;
  private Date createdAt;
  private Date updatedAt;

}
