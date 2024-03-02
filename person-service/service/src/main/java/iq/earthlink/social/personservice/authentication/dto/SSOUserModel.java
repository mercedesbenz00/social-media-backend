package iq.earthlink.social.personservice.authentication.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iq.earthlink.social.personservice.authentication.enumeration.ProviderName;
import iq.earthlink.social.personservice.data.Gender;
import lombok.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SSOUserModel {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Gender gender = Gender.UNKNOWN;
    private Date birthday;
    private ProviderName providerName;

    public void setGender(String gender) {
        this.gender = Gender.valueOf(gender.toUpperCase());
    }

    public void setBirthday(String birthday) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        try {
            this.birthday = formatter.parse(birthday);
        } catch (ParseException ex) {
            this.birthday = null;
        }
    }
}
