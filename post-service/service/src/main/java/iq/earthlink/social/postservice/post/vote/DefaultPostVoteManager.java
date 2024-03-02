package iq.earthlink.social.postservice.post.vote;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.classes.enumeration.PostEventType;
import iq.earthlink.social.classes.enumeration.VoteType;
import iq.earthlink.social.common.data.event.PostActivityEvent;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.postservice.event.KafkaProducerService;
import iq.earthlink.social.postservice.event.KafkaTopics;
import iq.earthlink.social.postservice.group.GroupManager;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.PostManager;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettingsManager;
import iq.earthlink.social.postservice.post.rest.JsonPost;
import iq.earthlink.social.postservice.post.rest.PostNotificationSettingsDTO;
import iq.earthlink.social.postservice.post.vote.repository.PostVoteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
@RequiredArgsConstructor
public class DefaultPostVoteManager implements PostVoteManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPostVoteManager.class);

    private final PostManager postManager;
    private final PostNotificationSettingsManager postNotificationSettingsManager;
    private final PostVoteRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final GroupManager groupManager;
    private final KafkaProducerService kafkaProducerService;
    private final Mapper mapper;

    @Override
    public VoteCount addPostVote(PersonDTO person, Post post, VoteType voteType) {
        savePostVote(person.getPersonId(), post, voteType.getType());
        List<VoteCount> voteCounts = getPostVoteCounts(person.getPersonId(), Collections.singletonList(post.getId()));
        boolean isMuted = false;
        try {
            PostNotificationSettingsDTO setting = postNotificationSettingsManager.findPostNotificationSettingsByPostId(post.getAuthorId(), post.getId());
            isMuted = setting.isMuted();
        } catch (NotFoundException ex) {
            // if setting is not found, keep isMuted = false
        }
        if (!isMuted) {
            // Send notification if Author did not turn off notification for the post:
            var group = groupManager.getGroupById(post.getUserGroupId());
            PersonData eventAuthor = PersonData
                    .builder()
                    .id(person.getPersonId())
                    .displayName(person.getDisplayName())
                    .avatar(person.getAvatar())
                    .build();
            postManager.sendNotification(null, eventAuthor, post, group, NotificationType.POST_VOTE_ADDED, Map.of("voteType", voteType.name()), false);
        }
        return CollectionUtils.isNotEmpty(voteCounts) ? voteCounts.get(0) : null;
    }

    @Override
    public List<VoteCount> getPostVoteCounts(Long personId, Collection<Long> postIds) {
        List<VoteCount> postVoteCounts = repository.getPostVoteCounts(postIds);

        if (CollectionUtils.isNotEmpty(postVoteCounts)) {
            List<PostVote> votes = getPostVotesForPerson(personId, postIds);
            if (CollectionUtils.isNotEmpty(votes)) {
                Map<Long, Integer> votesMap = votes.stream().collect(Collectors.toMap(v -> v.getId().getPost().getId(), PostVote::getVoteType));
                for (VoteCount pvc : postVoteCounts) {
                    int personVote = votesMap.get(pvc.getId()) != null ? votesMap.get(pvc.getId()) : 0;
                    pvc.setVoteValue(personVote);
                }
            }
        }
        return postVoteCounts;
    }

    @Transactional
    @Override
    public VoteCount deletePostVote(Long personId, Post post) {
        List<PostVote> postVotes = repository.getPersonPostVotesForPosts(personId, Collections.singletonList(post.getId()));
        if (CollectionUtils.isNotEmpty(postVotes)) {
            repository.removePostVote(personId, post.getId());

            String eventType = VoteType.UPVOTE.equals(VoteType.getVoteType(postVotes.get(0).getVoteType())) ? PostEventType.POST_UPVOTE_REMOVED.name()
                    : PostEventType.POST_DOWNVOTE_REMOVED.name();
            PostActivityEvent.send(rabbitTemplate, Map.of(PostActivityEvent.POST_ID, post.getId(),
                    PostActivityEvent.POST_EVENT_TYPE, eventType));
        }
        List<VoteCount> pvCounts = repository.getPostVoteCounts(Collections.singletonList(post.getId()));
        return CollectionUtils.isNotEmpty(pvCounts) ? pvCounts.get(0) : null;
    }

    @Override
    public List<PostVote> getPostVotesForPerson(Long personId, Collection<Long> postIds) {
        return CollectionUtils.isNotEmpty(postIds) ? repository.getPersonPostVotesForPosts(personId, postIds)
                : repository.getAllPersonPostVotes(personId);
    }

    private void savePostVote(Long personId, Post post, int voteType) {
        checkNotNull(personId, "error.check.not.null", "personId");
        checkNotNull(post, "error.check.not.null", "post");

        PostVotePK pk = new PostVotePK(personId, post);
        PostVote postVote = repository.findByPrimaryKey(pk);

        int voteTypeOrig = 0;
        boolean updatePostVote = true;

        if (postVote == null) {
            postVote = new PostVote();
            postVote.setId(pk);
            updatePostVote = false;
        } else {
            voteTypeOrig = postVote.getVoteType();
        }

        postVote.setVoteType(voteType);

        LOGGER.debug("Saving the vote for post: {} from person: {} with voteType: {}", post, personId, VoteType.getVoteType(voteType));
        repository.save(postVote);

        String eventType = VoteType.UPVOTE.equals(VoteType.getVoteType(voteType)) ? PostEventType.POST_UPVOTE_ADDED.name()
                : PostEventType.POST_DOWNVOTE_ADDED.name();

        if (voteTypeOrig == voteType) {
            return;
        }

        if (eventType.equals(PostEventType.POST_UPVOTE_ADDED.name())){
            String key = post.getUserGroupId() + "_" + post.getAuthorId();
            JsonPost jsonPost = mapper.map(post, JsonPost.class);
            kafkaProducerService.sendMessage(String.valueOf(KafkaTopics.POST_LIKED), key, jsonPost);
        }

        PostActivityEvent.send(rabbitTemplate, Map.of(PostActivityEvent.POST_ID, post.getId(),
                PostActivityEvent.POST_EVENT_TYPE, eventType));
        if (updatePostVote) {
            eventType = VoteType.UPVOTE.equals(VoteType.getVoteType(voteType)) ? PostEventType.POST_DOWNVOTE_REMOVED.name()
                    : PostEventType.POST_UPVOTE_REMOVED.name();
            PostActivityEvent.send(rabbitTemplate, Map.of(PostActivityEvent.POST_ID, post.getId(),
                    PostActivityEvent.POST_EVENT_TYPE, eventType));
        }
    }
}
