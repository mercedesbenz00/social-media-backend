package iq.earthlink.social.groupservice.group.rest;

import lombok.Data;

@Data
public class JsonGroupMemberWithNotificationSettings {
  private Long personId;
  private Long groupId;
  private boolean isMuted;
}
