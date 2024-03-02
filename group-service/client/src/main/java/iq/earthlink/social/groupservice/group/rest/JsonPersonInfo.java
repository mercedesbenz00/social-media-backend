package iq.earthlink.social.groupservice.group.rest;

import iq.earthlink.social.classes.enumeration.RegistrationState;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JsonPersonInfo {

    private Long personId;
    private String displayName;
    private RegistrationState state;

    public JsonPersonInfo(RegistrationState state) {
        this.state = state;
    }
}
