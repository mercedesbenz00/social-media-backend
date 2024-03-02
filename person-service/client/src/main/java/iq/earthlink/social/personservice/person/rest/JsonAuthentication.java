package iq.earthlink.social.personservice.person.rest;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class JsonAuthentication {
    private String token;
    private String refreshToken;
    private Date expireTime;
}
