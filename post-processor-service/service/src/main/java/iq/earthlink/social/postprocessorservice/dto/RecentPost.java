package iq.earthlink.social.postprocessorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecentPost {
    private String postUuid;
    private Date publishedAt;
}
