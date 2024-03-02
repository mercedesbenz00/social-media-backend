package iq.earthlink.social.personservice.person.model;

import iq.earthlink.social.common.file.MediaFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportedPersonImpl implements ReportedPerson {

    private Long id;
    private String displayName;
    private MediaFile avatar;
    private Date createdAt;
    private Long postCount;
    private Boolean isBanned;
    private Date banExpiresAt;
}
