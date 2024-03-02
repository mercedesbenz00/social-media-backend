package iq.earthlink.social.personservice.person.rest;

import lombok.Data;

import java.util.Date;

@Data
public class JsonFollowing {
  private Long id;
  private JsonPerson subscriber;
  private JsonPerson subscribedTo;
  private Date createdAt;
}
