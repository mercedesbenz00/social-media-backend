package iq.earthlink.social.postservice.story.model;

import iq.earthlink.social.classes.enumeration.StoryAccessType;
import iq.earthlink.social.postservice.story.view.StoryView;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "story_author_idx", columnList = "authorId")
})
@EntityListeners(AuditingEntityListener.class)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "story_seq_gen")
    @SequenceGenerator(name = "story_seq_gen", sequenceName = "story_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    private Long authorId;

    @NotNull
    @CreatedDate
    private Date createdAt;

    /**
     * Comma separated list of reference person ids.
     */
    private String personReferences;

    @OneToMany(mappedBy = "story", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<StoryView> views;

    @Enumerated(EnumType.STRING)
    private StoryAccessType accessType = StoryAccessType.ALL_FOLLOWERS;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "story_allowed_viewers", joinColumns = @JoinColumn(name = "story_id"))
    @Column(name = "personId")
    private Set<Long> selectedFollowerIds = new HashSet<>();

}
