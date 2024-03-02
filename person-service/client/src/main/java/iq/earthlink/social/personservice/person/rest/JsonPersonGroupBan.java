package iq.earthlink.social.personservice.person.rest;

import lombok.Data;

import java.util.Date;

@Data
public class JsonPersonGroupBan {

  private Long id;

  private JsonPerson author;

  private JsonPerson bannedPerson;

  private Date createdAt;

  private Date expiredAt;

  private String reason;

  private Long userGroupId;
}
