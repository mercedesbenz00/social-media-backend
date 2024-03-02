package iq.earthlink.social.classes.enumeration;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

public enum VoteType {
    UPVOTE(1), DOWNVOTE(2);

    private static final Logger LOGGER = LoggerFactory.getLogger(VoteType.class);

    VoteType(int voteType) {
        this.type = voteType;
    }

    @Getter
    private final int type;

    public static VoteType getVoteType(int voteType) {
        Optional<VoteType> types = Arrays.stream(values()).filter(it -> it.type == voteType).findFirst();
        if (types.isPresent()) {
            return types.get();
        }
        LOGGER.debug("Wrong vote type is provided: {}.  Valid vote type values: 1 (upvote) or 2 (downvote)", voteType);
        return null;
    }
}
