package iq.earthlink.social.postservice.post.notificationsettings;

import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.rest.JsonPostNotificationSettings;
import iq.earthlink.social.postservice.post.rest.PostNotificationSettingsDTO;
import org.dozer.Mapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class DefaultPostNotificationSettingsManager implements PostNotificationSettingsManager {

    private final PostNotificationSettingsRepository postNotificationSettingsRepository;
    private final PostManager postManager;
    private final Mapper mapper;

    public DefaultPostNotificationSettingsManager(PostNotificationSettingsRepository postNotificationSettingsRepository, Mapper mapper, PostManager postManager) {
        this.postNotificationSettingsRepository = postNotificationSettingsRepository;
        this.mapper = mapper;
        this.postManager = postManager;
    }

    @NotNull
    @Override
    public Page<PostNotificationSettingsDTO> findPostNotificationSettings(@NotNull Long personId, List<Long> postIds, @NotNull Pageable page) {
        if (CollectionUtils.isEmpty(postIds))
            return postNotificationSettingsRepository.findByPersonId(personId, page).map(postNotificationSettings ->
                    PostNotificationSettingsDTO.builder()
                            .postId(postNotificationSettings.getPostId())
                            .isMuted(postNotificationSettings.isMuted())
                            .build());
        else
            return postNotificationSettingsRepository.findByPersonIdAndPostIdIn(personId, postIds, page).map(postNotificationSettings ->
                    PostNotificationSettingsDTO.builder()
                            .postId(postNotificationSettings.getPostId())
                            .isMuted(postNotificationSettings.isMuted())
                            .build());
    }

    @NotNull
    @Override
    public PostNotificationSettingsDTO findPostNotificationSettingsByPostId(Long personId, Long postId) {
        PostNotificationSettings postNotificationSettings = getPostNotificationSettings(personId, postId);
        return mapper.map(postNotificationSettings, PostNotificationSettingsDTO.class);
    }

    @Override
    public PostNotificationSettingsDTO setPostNotificationSettings(Long personId, Long postId, JsonPostNotificationSettings request) {
        postManager.getPost(postId);
        PostNotificationSettings postNotificationSettings = postNotificationSettingsRepository.findByPersonIdAndPostId(personId, postId)
                .orElse(new PostNotificationSettings(personId, postId));

        postNotificationSettings.setMuted(request.getIsMuted());
        postNotificationSettingsRepository.save(postNotificationSettings);

        return mapper.map(postNotificationSettings, PostNotificationSettingsDTO.class);
    }

    private PostNotificationSettings getPostNotificationSettings(Long personId, Long postId) {
        return postNotificationSettingsRepository.findByPersonIdAndPostId(personId, postId)
                .orElseThrow(() -> new NotFoundException("error.not.found.post.notification.settings", personId, postId));
    }
}

