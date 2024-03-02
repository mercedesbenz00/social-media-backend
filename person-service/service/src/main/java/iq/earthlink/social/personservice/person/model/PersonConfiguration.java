package iq.earthlink.social.personservice.person.model;

import iq.earthlink.social.classes.enumeration.LocalizationType;
import iq.earthlink.social.classes.enumeration.ThemeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_configuration_gen")
    @SequenceGenerator(name = "person_configuration_gen", sequenceName = "person_configuration_gen", allocationSize = 1)
    private Long id;

    private Long personId;

    private boolean notificationMute;

    @Enumerated(EnumType.STRING)
    private LocalizationType localization;

    @Enumerated(EnumType.STRING)
    private ThemeType theme;
}
