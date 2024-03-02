package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.groupservice.group.member.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
@RequiredArgsConstructor
public class GroupManagerUtils {
    private final GroupRepository repository;
    private final GroupMemberRepository groupMemberRepository;

    private static final String GROUP_ID = "groupId";

    @Nonnull
    public UserGroup getGroup(@Nonnull Long groupId) {
        checkNotNull(groupId, ERROR_CHECK_NOT_NULL, GROUP_ID);

        return repository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("error.group.not.found", groupId));
    }

    @Nonnull
    public Long getPendingJoinRequests(@Nonnull Long groupId) {
        checkNotNull(groupId, ERROR_CHECK_NOT_NULL, GROUP_ID);

        return groupMemberRepository.countPendingJoinRequests(groupId);
    }

    @Nonnull
    public Long getGroupMembers(@Nonnull Long groupId) {
        checkNotNull(groupId, ERROR_CHECK_NOT_NULL, GROUP_ID);

        return groupMemberRepository.countGroupMembers(groupId);
    }
}
