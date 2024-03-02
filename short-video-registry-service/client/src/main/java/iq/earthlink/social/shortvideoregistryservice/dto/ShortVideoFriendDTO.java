package iq.earthlink.social.shortvideoregistryservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortVideoFriendDTO {
    private Long userId;
    private String authorUserName;
}