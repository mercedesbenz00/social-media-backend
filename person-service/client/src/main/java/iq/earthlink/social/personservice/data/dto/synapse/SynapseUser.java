package iq.earthlink.social.personservice.data.dto.synapse;

import lombok.*;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SynapseUser {
    private String id;
    private String password;
}

