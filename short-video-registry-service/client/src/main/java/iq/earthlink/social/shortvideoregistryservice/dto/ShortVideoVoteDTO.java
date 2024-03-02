package iq.earthlink.social.shortvideoregistryservice.dto;

import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortVideoVoteDTO {
    private long personId;
    private UUID id;
    private int voteType;
    private Timestamp createdAt;
}
