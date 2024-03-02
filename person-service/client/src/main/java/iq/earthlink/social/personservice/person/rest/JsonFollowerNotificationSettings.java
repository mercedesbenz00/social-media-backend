package iq.earthlink.social.personservice.person.rest;

import lombok.Data;

@Data
public class JsonFollowerNotificationSettings {

  private Long followingId;
  private Boolean isMuted;
}