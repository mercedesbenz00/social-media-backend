package iq.earthlink.social.classes.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonData implements Serializable {
    private Long id;
    private String displayName;
    private JsonMediaFile avatar;
}
