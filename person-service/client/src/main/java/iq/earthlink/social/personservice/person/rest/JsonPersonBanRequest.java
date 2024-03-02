package iq.earthlink.social.personservice.person.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.personservice.person.PersonBanData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonPersonBanRequest implements PersonBanData {

    @ApiModelProperty("ID of the person, who should be banned")
    private Long personId;

    @ApiModelProperty("Predefined reason ID")
    private Long reasonId;

    @ApiModelProperty("Custom reason why person should be banned")
    private String reason;

    @ApiModelProperty("How many days person should be banned")
    private int days;

    @ApiModelProperty("User groups to be banned")
    private List<Long> groupIds;
}
