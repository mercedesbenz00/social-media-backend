package iq.earthlink.social.userfeedaggregatorservice.service;

import io.micrometer.core.annotation.Timed;
import iq.earthlink.social.groupservice.group.rest.MembersRestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserGroupsServiceImpl implements UserGroupsService {

    private final MembersRestService membersRestService;

    @Override
    @Timed(value="userFeed.get.group.id.list", description = "Time taken to return the list of user group Ids")
    public Set<Long> getUserGroupIdList(String authorizationHeader) {
        return membersRestService.getMyGroups(authorizationHeader, null);
    }
}
