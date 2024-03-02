package iq.earthlink.social.classes.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDetails {
    private Long id;
    private UUID uuid;
    private Boolean isVerified;
    private String displayName;
    private AvatarDetails avatar;

}
