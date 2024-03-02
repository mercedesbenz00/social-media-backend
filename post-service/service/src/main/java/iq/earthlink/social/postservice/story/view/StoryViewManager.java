package iq.earthlink.social.postservice.story.view;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface StoryViewManager {

  @NotNull
  StoryView save(Long storyId, Long personId);

  @NotNull
  List<StoryView> findViews(Long personId);
}
