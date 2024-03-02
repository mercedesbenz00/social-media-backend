package iq.earthlink.social.postservice.story.model;

import iq.earthlink.social.classes.enumeration.StoryAccessType;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Builder
@Entity
@Getter
@Setter
@Table(uniqueConstraints={@UniqueConstraint(columnNames = {"personId"})})
@NoArgsConstructor
@AllArgsConstructor
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class StoryConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "story_config_seq_gen")
    @SequenceGenerator(name = "story_config_seq_gen", sequenceName = "story_config_seq_gen", allocationSize = 1)
    private Long id;

    Long personId;

    @Enumerated(EnumType.STRING)
    private StoryAccessType accessType = StoryAccessType.ALL_FOLLOWERS;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "story_configuration_allowed_users", joinColumns = @JoinColumn(name = "story_configuration_id"))
    @Column(name = "personId")
    private Set<Long> allowedFollowersIds = new HashSet<>();
}
