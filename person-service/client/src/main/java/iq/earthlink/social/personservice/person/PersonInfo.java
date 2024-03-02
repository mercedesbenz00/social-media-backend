package iq.earthlink.social.personservice.person;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonViews;
import iq.earthlink.social.classes.enumeration.RegistrationState;

import java.util.Set;

public interface PersonInfo extends PersonData {

  Long getId();

  String getUsername();

  String getEmail();

  Set<String> getRoles();

  Set<String> getAuthorities();

  long getFollowerCount();

  long getFollowingCount();

  long getPostCount();

  long getGroupCount();

  long getInterestCount();

  JsonMediaFile getCover();

  JsonMediaFile getAvatar();

  RegistrationState getState();

  @ApiModelProperty(hidden = true)
  @JsonView({JsonViews.Internal.class})
  default boolean isAdmin() {
    return getRoles() != null && getRoles().contains("ADMIN");
  }
}
