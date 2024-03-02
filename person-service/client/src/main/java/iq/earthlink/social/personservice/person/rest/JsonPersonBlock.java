package iq.earthlink.social.personservice.person.rest;

import lombok.Data;

import java.util.Date;

@Data
public class JsonPersonBlock {

  private Long id;

  private JsonPerson person;

  private JsonPerson blockedPerson;

  private Date createdAt;
}
