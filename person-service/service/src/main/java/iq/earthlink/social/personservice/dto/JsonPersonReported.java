package iq.earthlink.social.personservice.dto;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonPersonReported {

    private Long id;
    private String displayName;
    private JsonMediaFile avatar;
    private long postCount;
    private Date createdAt;
    private long complaintCount;
    private long postComplaintCount;
    private long commentComplaintCount;
    private Date lastComplaintDate;
    private Boolean isBanned;
    private Date banExpiresAt;
}
