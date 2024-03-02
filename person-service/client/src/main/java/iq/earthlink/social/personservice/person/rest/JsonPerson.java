package iq.earthlink.social.personservice.person.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JsonPerson {

    private Long id;
    private String displayName;
    private long followerCount;
    private long followingCount;
    private long postCount;
    private long groupCount;
    private long interestCount;
    private Boolean isVerifiedAccount;
    private JsonMediaFile cover;
    private JsonMediaFile avatar;
    @JsonProperty("isFollowing")
    private boolean isFollowing;
    private String bio;

}
