package iq.earthlink.social.classes.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonImageParameters {
    int width;
    int height;
    boolean stretch;
    boolean smart;
    List<JsonImageFilter> filters;
}
