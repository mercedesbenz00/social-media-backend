package iq.earthlink.social.shortvideoregistryservice.dto;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortVideoCategoryDTO {
    @NotNull
    private UUID categoryId;
    @NotNull
    private String name;
}