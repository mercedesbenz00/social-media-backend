package iq.earthlink.social.postservice.post.vote;

import iq.earthlink.social.classes.data.dto.PersonData;
import iq.earthlink.social.classes.enumeration.NotificationType;
import iq.earthlink.social.classes.enumeration.PostEventType;
import iq.earthlink.social.classes.enumeration.VoteType;
import iq.earthlink.social.common.data.event.PostActivityEvent;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.postservice.group.GroupManager;
import iq.earthlink.social.postservice.group.dto.GroupDTO;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.comment.Comment;
import iq.earthlink.social.postservice.post.comment.CommentService;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.notificationsettings.PostNotificationSettingsManager;
import iq.earthlink.social.postservice.post.rest.JsonVoteCount;
import iq.earthlink.social.postservice.post.rest.PostNotificationSettingsDTO;
import iq.earthlink.social.postservice.post.vote.repository.CommentVoteRepository;
import iq.earthlink.social.postservice.util.CommentUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
@RequiredArgsConstructor
public class DefaultCommentVoteManager implements CommentVoteManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCommentVoteManager.class);

    private final CommentService commentService;
    private final CommentVoteRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final PostNotificationSettingsManager postNotificationSettingsManager;
    private final GroupManager groupManager;
    private final CommentUtil commentUtil;
    private final Mapper mapper;

    @Override
    public VoteCount addCommentVote(PersonDTO person, UUID commentUuid, VoteType voteType) {
        Comment comment = commentService.getCommentByUuidInternal(commentUuid);
        saveCommentVote(person.getPersonId(), comment, voteType.getType());
        Post post = comment.getPost();
        boolean isMuted = false;
        try {
            PostNotificationSettingsDTO setting = postNotificationSettingsManager.findPostNotificationSettingsByPostId(comment.getAuthorId(), comment.getPost().getId());
            isMuted = setting.isMuted();
        } catch (NotFoundException ex) {
            // if setting is not found, keep isMuted = false
        }
        if (!isMuted) {
            GroupDTO group = groupManager.getGroupById(post.getUserGroupId());
            PersonData eventAuthor = PersonData
                    .builder()
                    .id(person.getPersonId())
                    .avatar(person.getAvatar())
                    .displayName(person.getDisplayName())
                    .build();
            commentService.sendNotification(eventAuthor, group, post, comment, NotificationType.COMMENT_VOTE_ADDED,
                    null, Map.of("voteType", voteType.name()));
        }

        return commentUtil.getCommentVoteCounts(person.getPersonId(), Collections.singletonList(comment.getId())).get(0);
    }

    @Transactional
    @Override
    public JsonVoteCount deleteCommentVote(Long personId, UUID commentUuid) {
        Comment comment = commentService.getCommentByUuidInternal(commentUuid);
        List<CommentVote> commentVotes = repository.getPersonVotesForComments(personId, Collections.singletonList(comment.getId()));

        if (CollectionUtils.isNotEmpty(commentVotes)) {
            repository.removeCommentVote(personId, comment.getId());

            String eventType = VoteType.UPVOTE.equals(VoteType.getVoteType(commentVotes.get(0).getVoteType())) ? PostEventType.POST_COMMENT_UPVOTE_REMOVED.name()
                    : PostEventType.POST_COMMENT_DOWNVOTE_REMOVED.name();
            PostActivityEvent.send(rabbitTemplate, Map.of(PostActivityEvent.POST_ID, comment.getPost().getId(),
                    PostActivityEvent.POST_EVENT_TYPE, eventType));
        }

        List<VoteCount> voteCounts = repository.getCommentVoteCounts(Collections.singletonList(comment.getId()));
        return CollectionUtils.isNotEmpty(voteCounts) ?
                mapper.map(voteCounts.get(0), JsonVoteCount.class) :
                new JsonVoteCount(comment.getId(), 0L, 0L, 0);
    }

    private void saveCommentVote(Long personId, Comment comment, Integer voteType) {
        checkNotNull(personId, "error.check.not.null", "personId");
        checkNotNull(comment, "error.check.not.null", "comment");

        CommentVotePK pk = new CommentVotePK(personId, comment);
        CommentVote commentVote = repository.findByPrimaryKey(pk);

        int voteTypeOrig = 0;
        boolean updatePostCommentVote = false;
        if (commentVote == null) {
            commentVote = new CommentVote();
            commentVote.setId(pk);
        } else {
            updatePostCommentVote = true;
            voteTypeOrig = commentVote.getVoteType();
        }

        commentVote.setVoteType(voteType);

        LOGGER.debug("Saving the vote for post: {} from person: {} with voteType: {}", comment, personId, VoteType.getVoteType(voteType));
        repository.save(commentVote);

        String eventType = VoteType.UPVOTE.equals(VoteType.getVoteType(voteType)) ? PostEventType.POST_COMMENT_UPVOTE_ADDED.name()
                : PostEventType.POST_COMMENT_DOWNVOTE_ADDED.name();

        if (voteTypeOrig == voteType) {
            return;
        }

        PostActivityEvent.send(rabbitTemplate, Map.of(PostActivityEvent.POST_ID, comment.getPost().getId(),
                PostActivityEvent.POST_EVENT_TYPE, eventType));
        if (updatePostCommentVote) {
            eventType = VoteType.UPVOTE.equals(VoteType.getVoteType(voteType)) ? PostEventType.POST_COMMENT_DOWNVOTE_REMOVED.name()
                    : PostEventType.POST_COMMENT_UPVOTE_REMOVED.name();
            PostActivityEvent.send(rabbitTemplate, Map.of(PostActivityEvent.POST_ID, comment.getPost().getId(),
                    PostActivityEvent.POST_EVENT_TYPE, eventType));
        }
    }
}
