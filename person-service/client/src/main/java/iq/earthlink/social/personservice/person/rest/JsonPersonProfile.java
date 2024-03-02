package iq.earthlink.social.personservice.person.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.enumeration.RegistrationState;
import iq.earthlink.social.personservice.data.Gender;
import iq.earthlink.social.personservice.person.PersonInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JsonPersonProfile implements PersonInfo {

    private Long id;
    private UUID uuid;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Gender gender;

    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "The person birthdate as timestamp", example = "123123556342354", dataType = "Long.class")
    private Date birthDate;

    @ApiModelProperty(value = "The person global roles", example = "['ADMIN'], ['USER']")
    private Set<String> roles;

    @ApiModelProperty(hidden = true)
    private Set<String> authorities;

    private String displayName;
    private Long cityId;

    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "Account deleted date as timestamp", example = "123123556342354", dataType = "Long.class")
    private Date deletedDate;

    private long followerCount;
    private long followingCount;
    private long postCount;
    private long groupCount;
    private long interestCount;
    private Boolean isVerifiedAccount;
    private Boolean isRegistrationCompleted;
    private JsonMediaFile cover;
    private JsonMediaFile avatar;
    private RegistrationState state;
    private String bio;

    @ApiModelProperty(value = "The person joined as timestamp", example = "123123556342354", dataType = "Long.class")
    private Date createdAt;

}
