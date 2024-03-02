package iq.earthlink.social.feedaggregatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO implements Serializable {
  private String postUuid;
  private Long userGroupId;
}
