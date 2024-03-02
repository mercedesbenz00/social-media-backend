package iq.earthlink.social.postservice.util;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.postservice.post.PostMediaService;
import iq.earthlink.social.postservice.post.rest.JsonPost;
import iq.earthlink.social.postservice.post.rest.JsonVoteCount;
import iq.earthlink.social.postservice.post.statistics.PostStatistics;
import iq.earthlink.social.postservice.post.statistics.PostStatisticsManager;
import iq.earthlink.social.postservice.post.vote.PostVote;
import iq.earthlink.social.postservice.post.vote.repository.PostVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostStatisticsAndMediaUtil {
    private final PostMediaService mediaService;
    private final PostStatisticsManager postStatisticsManager;
    private final PostVoteRepository postVoteRepository;

    public JsonPost enrichWithStatisticsAndMedia(PersonInfo person, JsonPost jsonPost) {
        return enrichWithStatisticsAndMedia(person.getId(), jsonPost);
    }

    public JsonPost enrichWithStatisticsAndMedia(Long personId, JsonPost jsonPost) {
        List<JsonMediaFile> files = mediaService.findPostFilesWithFullPath(jsonPost.getId());
        jsonPost.setFiles(files);

        PostStatistics postStatistics = postStatisticsManager.getPostStatisticsByPostId(jsonPost.getId());
        List<PostVote> vote = postVoteRepository.getPersonPostVotesForPosts(personId, List.of(jsonPost.getId()));
        if (postStatistics != null) {
            jsonPost.setLastActivityAt(postStatistics.getLastActivityAt());
            jsonPost.setScore(postStatistics.getScore());
            jsonPost.setCommentsCount(postStatistics.getCommentsCount());
            jsonPost.setTotalVotes(JsonVoteCount.builder()
                    .id(jsonPost.getId())
                    .upvotesTotal(postStatistics.getUpvotesCount())
                    .downvotesTotal(postStatistics.getDownvotesCount())
                    .voteValue(!vote.isEmpty() ? vote.get(0).getVoteType() : 0)
                    .build());
        }
        return jsonPost;
    }
}
