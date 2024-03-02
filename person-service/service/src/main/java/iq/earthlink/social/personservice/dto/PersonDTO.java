package iq.earthlink.social.personservice.dto;

import lombok.Data;

import java.util.Date;
import java.util.Set;

@Data
public class PersonDTO {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String displayName;
    private Date birthDate;
    private Date createdAt;
    private String email;
    private Long cityId;
    private String password;
    private Set<String> roles;
    private String language;
    private Date deletedDate;
    private Integer resetCode;
    private Date resetCodeExpireAt;
    private String confirmCode;

    public boolean isAdmin() {
        return getRoles().contains("ADMIN");
    }
}
