package iq.earthlink.social.personservice.person.rest;

import iq.earthlink.social.classes.enumeration.TimeInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserStats {

  private long allUsersCount;
  private long newUsersCount;
  private List<ActivatedUsers> activatedUsers;
  private TimeInterval timeInterval;
  private Timestamp fromDate;

}

