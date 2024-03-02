package iq.earthlink.social.postprocessorservice.dto;

import lombok.Data;

import java.util.Date;

@Data
public class PostEvent {
  private String postUuid;
  private Date publishedAt;
  private Long groupId;
  private PostEventType eventType;
}
