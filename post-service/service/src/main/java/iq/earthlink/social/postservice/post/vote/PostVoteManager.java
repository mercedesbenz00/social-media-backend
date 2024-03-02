package iq.earthlink.social.postservice.post.vote;

import iq.earthlink.social.classes.enumeration.VoteType;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.model.Post;

import java.util.Collection;
import java.util.List;

public interface PostVoteManager {

    VoteCount addPostVote(PersonDTO person, Post post, VoteType voteType);

    List<VoteCount> getPostVoteCounts(Long personId, Collection<Long> postIds);

    VoteCount deletePostVote(Long personId, Post post);

    List<PostVote> getPostVotesForPerson(Long personId, Collection<Long> postIds);
}
