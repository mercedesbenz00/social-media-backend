package iq.earthlink.social.postservice.post.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatsDTO {

    private long postComplaintsCount;
    private long commentComplaintsCount;
}
