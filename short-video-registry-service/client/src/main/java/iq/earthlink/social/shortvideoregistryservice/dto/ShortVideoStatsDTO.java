package iq.earthlink.social.shortvideoregistryservice.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortVideoStatsDTO {

    private UUID id;
    private long likes;
    private long dislikes;
    private long views;
    private long skips;
    private long halves;
    private long comments;
}
