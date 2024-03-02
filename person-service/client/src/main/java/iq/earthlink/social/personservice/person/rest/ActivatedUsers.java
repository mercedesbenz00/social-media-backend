package iq.earthlink.social.personservice.person.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivatedUsers {

  private String date;
  private long activatedUsersCount;

}

