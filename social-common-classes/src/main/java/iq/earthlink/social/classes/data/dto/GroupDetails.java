package iq.earthlink.social.classes.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDetails {
    private Long id;
    private String name;
    private AvatarDetails avatar;
}
