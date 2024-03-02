package iq.earthlink.social.groupservice.group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardDTO {
    private Long personId;
    private OnboardState state;
}
