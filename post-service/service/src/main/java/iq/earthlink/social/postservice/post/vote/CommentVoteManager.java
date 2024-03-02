package iq.earthlink.social.postservice.post.vote;

import iq.earthlink.social.classes.enumeration.VoteType;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.rest.JsonVoteCount;

import java.util.UUID;

public interface CommentVoteManager {

    VoteCount addCommentVote(PersonDTO person, UUID commentUuid, VoteType voteType);
    JsonVoteCount deleteCommentVote(Long personId, UUID commentUuid);
}
