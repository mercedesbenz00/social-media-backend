package iq.earthlink.social.personservice.data.dto.synapse;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
public class SynapseAuth {
    private boolean success;
    private String mxid;
    private SynapseProfile profile;
}
