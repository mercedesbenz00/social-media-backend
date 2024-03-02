package iq.earthlink.social.postservice.story;

import iq.earthlink.social.postservice.story.model.StoryConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoryConfigurationRepository extends JpaRepository<StoryConfiguration, Long> {

    Optional<StoryConfiguration> findByPersonId(Long personId);
}
