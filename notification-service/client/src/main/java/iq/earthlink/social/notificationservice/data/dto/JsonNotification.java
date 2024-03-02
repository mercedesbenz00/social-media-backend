package iq.earthlink.social.notificationservice.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.enumeration.NotificationState;
import iq.earthlink.social.classes.enumeration.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@ApiModel("Notification Object")
public class JsonNotification {

    @ApiModelProperty("Notification id")
    private String id;

    @ApiModelProperty("Receiver person id")
    @NotNull
    private Long receiverId;

    @ApiModelProperty("Notification state: NEW, READ, or DELETED")
    private NotificationState state;

    @ApiModelProperty("Notification text")
    private String body;

    @ApiModelProperty("Notification topic")
    @NotNull
    private NotificationType topic;

    @ApiModelProperty("Notification created date")
    private Date createdDate;

    @ApiModelProperty("Notification updated date")
    private Date updatedDate;

    @JsonProperty("metadata")
    @Valid
    private Map<String, String> metadata = null;

    @JsonProperty("eventAuthor")
    @Valid
    private PersonData eventAuthor = null;
}
