package iq.earthlink.social.postservice.story.view;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StoryViewRepository extends JpaRepository<StoryView, Long> {

  @Query("SELECT v FROM StoryView v WHERE v.story.id = ?1 AND v.viewerId = ?2")
  Optional<StoryView> findView(Long storyId, Long personId);

  @Query("SELECT v FROM StoryView v WHERE v.viewerId = ?1")
  List<StoryView> findViews(Long personId);
}
