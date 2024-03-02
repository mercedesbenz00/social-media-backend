package iq.earthlink.social.postservice.post.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class JsonReasonWithLocalization extends JsonReason {
    private Map<String, String> localizations = new HashMap<>();
}
