package iq.earthlink.social.personservice.data.dto.synapse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SynapsePasswordResponse {
    private SynapseAuth auth;
}
