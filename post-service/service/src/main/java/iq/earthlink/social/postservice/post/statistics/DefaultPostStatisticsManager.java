package iq.earthlink.social.postservice.post.statistics;

import iq.earthlink.social.classes.data.dto.PostDetailStatistics;
import iq.earthlink.social.classes.enumeration.PostEventType;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.common.data.event.PostActivityEvent;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.comment.complaint.repository.CommentComplaintRepository;
import iq.earthlink.social.postservice.post.complaint.repository.PostComplaintRepository;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import iq.earthlink.social.postservice.post.rest.ComplaintStatsDTO;
import iq.earthlink.social.postservice.post.rest.GroupStatsDTO;
import iq.earthlink.social.postservice.post.rest.PostStatisticsGap;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.vladmihalcea.hibernate.util.LogUtils.LOGGER;

@Service
@RequiredArgsConstructor
public class DefaultPostStatisticsManager implements PostStatisticsManager {

    private static final String ERROR_OPERATION_NOT_PERMITTED = "error.operation.not.permitted";

    private final PostRepository postRepository;
    private final PostStatisticsRepository repository;
    private final PostComplaintRepository postComplaintRepository;
    private final CommentComplaintRepository commentComplaintRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public PostStatistics getPostStatisticsByPostId(Long postId) {

        return repository.findByPostId(postId).orElse(new PostStatistics());
    }

    @Override
    public List<PostDetailStatistics> getPostStatisticsByPostIds(List<Long> postIds) {
        var postStats = repository.findByPostIdIn(postIds);
        return postStats.stream()
                .map(postStat -> PostDetailStatistics
                        .builder()
                        .postId(postStat.getPost().getId())
                        .score(postStat.getScore())
                        .commentsCount(postStat.getCommentsCount())
                        .downvotesCount(postStat.getDownvotesCount())
                        .upvotesCount(postStat.getUpvotesCount())
                        .build())
                .toList();
    }

    @Override
    public void savePostStatistics(PostStatistics statistics) {
        repository.save(statistics);
    }

    @Override
    public void deletePostStatistics(Long postId) {
        repository.deletePostStatisticsByPostId(postId);
    }

    @Transactional
    @Override
    public void synchronizePostStatistics(@Nonnull PersonDTO person) {
        LOGGER.info("Synchronizing post statistics. ");
        if (!person.isAdmin()) {
            throw new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED);
        }
        try {
            PostActivityEvent.send(rabbitTemplate, Map.of(PostActivityEvent.POST_EVENT_TYPE, PostEventType.PURGE_STATS.name()));
            repository.synchronizePostStatistics();
            repository.updatePostScore();
        } catch (Exception ex) {
            LOGGER.error("Failed to synchronize post statistics, reason: {}", ex.getMessage());
        }
    }

    @Override
    public List<PostStatisticsGap> comparePostStatistics(PersonDTO person) {
        LOGGER.info("Comparing post statistics. ");
        if (!person.isAdmin()) {
            throw new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED);
        }
        try {
            return repository.findOutdatedPosts();
        } catch (Exception ex) {
            LOGGER.error("Failed to compare post statistics, reason: {}", ex.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public ComplaintStatsDTO getComplaintStatsByPerson(PersonDTO currentUser, Long personId) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED);
        }
        Long postComplaintCount = postComplaintRepository.getAllPostComplaintsCountByPerson(personId);
        Long commentComplaintCount = commentComplaintRepository.getAllCommentComplaintsCountByPerson(personId);
        return ComplaintStatsDTO
                .builder()
                .commentComplaintsCount(commentComplaintCount)
                .postComplaintsCount(postComplaintCount)
                .build();

    }

    @Override
    public GroupStatsDTO getGroupStats(PersonDTO currentUser, Long groupId) {
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException(ERROR_OPERATION_NOT_PERMITTED);
        }
        Long pendingPostsCount = postRepository.getPostsCountByStateAndGroup(PostState.WAITING_TO_BE_PUBLISHED, groupId);
        return GroupStatsDTO
                .builder()
                .pendingPostsCount(pendingPostsCount)
                .build();
    }
}
