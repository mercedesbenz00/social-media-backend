package iq.earthlink.social.postservice.util;

import iq.earthlink.social.postservice.post.vote.CommentVote;
import iq.earthlink.social.postservice.post.vote.VoteCount;
import iq.earthlink.social.postservice.post.vote.repository.CommentVoteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentUtil.class);

    private final CommentVoteRepository repository;

    public List<VoteCount> getCommentVoteCounts(Long personId, Collection<Long> commentIds) {
        List<VoteCount> commentVoteCounts = repository.getCommentVoteCounts(commentIds);
        if (CollectionUtils.isNotEmpty(commentVoteCounts)) {
            List<CommentVote> votes = getCommentVotesForPerson(personId, commentIds);
            if (CollectionUtils.isNotEmpty(votes)) {
                Map<Long, Integer> votesMap = votes.stream().collect(Collectors.toMap(v -> v.getId().getComment().getId(), CommentVote::getVoteType));
                for (VoteCount vc : commentVoteCounts) {
                    int personVote = votesMap.get(vc.getId()) != null ? votesMap.get(vc.getId()) : 0;
                    vc.setVoteValue(personVote);
                }
            }
        }
        return commentVoteCounts;
    }

    public List<CommentVote> getCommentVotesForPerson(Long personId, Collection<Long> commentIds) {
        return CollectionUtils.isNotEmpty(commentIds) ? repository.getPersonVotesForComments(personId, commentIds)
                : repository.getAllPersonCommentVotes(personId);
    }
}
