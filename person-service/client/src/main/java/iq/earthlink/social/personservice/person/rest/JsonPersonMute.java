package iq.earthlink.social.personservice.person.rest;

import lombok.Data;

import java.util.Date;

@Data
public class JsonPersonMute {
  private Long id;
  private JsonPerson person;
  private JsonPerson mutedPerson;
  private Date createdAt;
}
