package iq.earthlink.social.postservice.person.dto;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonEventDTO {
    private Long id;
    private UUID uuid;
    private String displayName;
    private Date createdAt;
    private Set<String> roles;
    private boolean isVerifiedAccount;
    private JsonMediaFile avatar;

    public boolean isAdmin() {
        return getRoles() != null && getRoles().contains("ADMIN");
    }
}
