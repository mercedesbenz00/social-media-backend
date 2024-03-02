package iq.earthlink.social.userfeedaggregatorservice.service;

import java.util.Set;

public interface UserGroupsService {

    Set<Long> getUserGroupIdList(String authorizationHeader);
}
