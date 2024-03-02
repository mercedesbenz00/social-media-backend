package iq.earthlink.social.postservice.story.view;

import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.postservice.story.StoryRepository;
import iq.earthlink.social.postservice.story.model.Story;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class DefaultStoryViewManager implements StoryViewManager {

    private final StoryViewRepository repository;
    private final StoryRepository storyRepository;

    public DefaultStoryViewManager(StoryViewRepository repository, StoryRepository storyRepository) {
        this.repository = repository;
        this.storyRepository = storyRepository;
    }

    @Transactional
    @Override
    public StoryView save(Long storyId, Long personId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("error.not.found.story", storyId));
        return repository.findView(story.getId(), personId)
                .map(v -> {
                    v.setUpdatedAt(new Date());
                    return repository.save(v);
                }).orElseGet(() -> {
                    StoryView v = StoryView.builder()
                            .viewerId(personId)
                            .story(story)
                            .build();
                    return repository.save(v);
                });
    }

    @Override
    public List<StoryView> findViews(Long personId) {
        return repository.findViews(personId);
    }
}
