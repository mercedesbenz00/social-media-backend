package iq.earthlink.social.personservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListMatrixPushTokenDTO {
    List<MatrixPushTokenDTO> pushers = new ArrayList<>();
}
