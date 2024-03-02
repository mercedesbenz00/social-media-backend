package iq.earthlink.social.groupservice.group.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateGroupRequests {

  private String date;
  private long createGroupRequestsCount;

}

