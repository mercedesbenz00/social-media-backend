package iq.earthlink.social.personservice.person.rest;

import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.personservice.person.ComplaintData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JsonComplaintData implements ComplaintData {

    @ApiModelProperty("Predefined complaint reason ID")
    private Long reasonId;

    @ApiModelProperty("Custom complaint reason")
    private String reason;

    @ApiModelProperty("Person who should be moderated")
    @NotNull
    private Long personId;

    @ApiModelProperty("If complaint is done for user group member")
    private Long userGroupId;
}
