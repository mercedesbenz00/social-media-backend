package iq.earthlink.social.personservice.person.rest;

import lombok.Data;

import java.util.Date;

@Data
public class JsonPersonBan {

  private Long id;

  private JsonPerson author;

  private JsonPersonProfile bannedPerson;

  private Date createdAt;

  private Date expiredAt;

  private String reason;

  private Long reasonId;
}
