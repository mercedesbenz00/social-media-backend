package iq.earthlink.social.personservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupInterestOnboardDTO {
    private Long personId;
    private GroupInterestOnboardState state;
}
