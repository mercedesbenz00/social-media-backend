package iq.earthlink.social.personservice.person.rest;

import lombok.Data;

import java.util.Date;

@Data
public class JsonPersonComplaint {

  private Long id;

  private JsonPerson owner;

  private JsonPerson person;

  private Long userGroupId;

  private Date createdAt;

  private Long reasonId;

  private String reason;

  private String state;

  private Long resolverId;

  private String resolvingText;

  private Date resolvingDate;
}
