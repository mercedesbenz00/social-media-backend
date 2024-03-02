package iq.earthlink.social.personservice.data.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class AppleIDTokenPayload {
    private String iss;
    private String aud;
    private Long exp;
    private Long iat;
    private String sub;// users unique id
    @SerializedName("at_hash")
    private String atHash;
    @SerializedName("auth_time")
    private Long authTime;
    @SerializedName("nonce_supported")
    private Boolean nonceSupported;
    @SerializedName("email_verified")
    private Boolean emailVerified;
    private String email;
}
