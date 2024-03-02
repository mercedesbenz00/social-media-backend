package iq.earthlink.social.personservice.service;

import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.personservice.person.FollowSearchCriteria;
import iq.earthlink.social.personservice.person.impl.DefaultFollowManager;
import iq.earthlink.social.personservice.person.impl.repository.FollowRepository;
import iq.earthlink.social.personservice.person.model.Following;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import lombok.NonNull;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


class DefaultFollowManagerTest {

    private static final String USER_1 = "User 1";
    private static final String USER_2 = "User 2";

    @InjectMocks
    private DefaultFollowManager followManager;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void follow_followYourself_throwBadRequestException() {
        Person follower = Person.builder()
                .id(1L)
                .build();

        Person followed = Person.builder()
                .id(1L)
                .build();

        assertThatThrownBy(() -> followManager.follow(follower, followed))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.follow.yourself.issue");
    }

    @Test
    void follow_updateFollowing_returnSavedFollowing() {
        Person follower = Person.builder()
                .id(1L)
                .personRoles(Set.of())
                .personAuthorities(Set.of())
                .build();

        Person followed = Person.builder()
                .id(2L)
                .personRoles(Set.of())
                .personAuthorities(Set.of())
                .build();

        Following following = Following.builder()
                .subscriber(follower)
                .subscribedTo(followed)
                .createdAt(new Date())
                .build();

        given(followRepository.saveAndFlush(any())).willReturn(following);

        Following savedFollowing = followManager.follow(follower, followed);
        assertEquals(following.getSubscriber().getId(), savedFollowing.getSubscriber().getId());
        assertEquals(following.getSubscribedTo().getId(), savedFollowing.getSubscribedTo().getId());
    }

    @Test
    void unfollow_deleteExistingFollowing_noErrorsAndDeleteFollowing() {
        Person follower = Person.builder()
                .id(1L)
                .build();

        Person followed = Person.builder()
                .id(2L)
                .build();

        Page<Following> page = new PageImpl<>(List.of(Following.builder().build()), PageRequest.of(0, 3), 1);
        given(followRepository.findFollowingBySubscribedToIdAndSubscriberIdIn(any(), any(), any())).willReturn(page);
        doNothing().when(followRepository).unfollow(any(), any());
        followManager.unfollow(follower.getId(), followed);
        verify(followRepository, times(1)).unfollow(follower.getId(), followed.getId());
    }

    @Test
    void unfollow_deleteNonExistingFollowing_noErrorsNoAction() {
        Person follower = Person.builder()
                .id(1L)
                .build();

        Person followed = Person.builder()
                .id(2L)
                .build();

        Page<Following> page = new PageImpl<>(List.of(), PageRequest.of(0, 3), 1);
        given(followRepository.findFollowingBySubscribedToIdAndSubscriberIdIn(any(), any(), any())).willReturn(page);
        doNothing().when(followRepository).unfollow(any(), any());
        followManager.unfollow(follower.getId(), followed);
        verify(followRepository, times(0)).unfollow(follower.getId(), followed.getId());
    }

    @Test
    void findFollowersOld_bySearchCriteria_returnPaginatedResult() {
        // 1. Find by subscribedTo person:
        FollowSearchCriteria criteria1 = FollowSearchCriteria.builder()
                .personId(1L)
                .build();

        List<Following> expectedBySubscribedTo = getFollowings().stream()
                .filter(p -> p.getSubscribedTo().getId().equals(criteria1.getPersonId())).collect(Collectors.toList());
        Page<Following> page1 = new PageImpl<>(expectedBySubscribedTo, PageRequest.of(0, 3), expectedBySubscribedTo.size());

        // given
        given(followRepository.findFollowers(criteria1, page1.getPageable())).willReturn(page1);

        Page<Following> actualBySubscribedTo = followManager.findFollowersOld(criteria1, page1.getPageable());

        assertTrue(actualBySubscribedTo.isFirst());
        assertEquals(2, actualBySubscribedTo.getTotalPages());
        assertEquals(expectedBySubscribedTo.size(), actualBySubscribedTo.getContent().size());
        assertEquals(expectedBySubscribedTo.size(), actualBySubscribedTo.getTotalElements());

        // 2. Find by subscribedTo and follower's display name:
        FollowSearchCriteria criteria2 = FollowSearchCriteria.builder()
                .personId(1L)
                .query(USER_2)
                .build();

        List<Following> expectedBySubscribedToAndDisplayName = getFollowings().stream()
                .filter(p -> p.getSubscribedTo().getId().equals(criteria2.getPersonId()) &&
                        p.getSubscriber().getDisplayName().contains(criteria2.getQuery())).collect(Collectors.toList());
        Page<Following> page2 = new PageImpl<>(expectedBySubscribedToAndDisplayName, PageRequest.of(0, 3),
                expectedBySubscribedToAndDisplayName.size());

        // given
        given(followRepository.findFollowers(criteria2, page2.getPageable())).willReturn(page2);

        Page<Following> actualBySubscribedToAndDisplayName = followManager.findFollowersOld(criteria2, page2.getPageable());

        assertTrue(actualBySubscribedToAndDisplayName.isFirst());
        assertEquals(1, actualBySubscribedToAndDisplayName.getTotalPages());
        assertEquals(expectedBySubscribedToAndDisplayName.size(), actualBySubscribedToAndDisplayName.getContent().size());
        assertEquals(expectedBySubscribedToAndDisplayName.size(), actualBySubscribedToAndDisplayName.getTotalElements());
    }

    @Test
    void findFollowers_bySearchCriteria_returnPaginatedResult() {
        FollowSearchCriteria criteria2 = FollowSearchCriteria.builder()
                .personId(1L)
                .query(USER_2)
                .build();

        List<Following> expectedFollowers = getFollowings().stream()
                .filter(p -> p.getSubscribedTo().getId().equals(criteria2.getPersonId()) &&
                        p.getSubscriber().getDisplayName().contains(criteria2.getQuery())).collect(Collectors.toList());

        List<Following> expectedFollowings = getFollowings().stream()
                .filter(p -> p.getSubscriber().getId().equals(criteria2.getPersonId())).collect(Collectors.toList());
        Page<Following> followers = new PageImpl<>(expectedFollowers, PageRequest.of(0, 3),
                expectedFollowers.size());
        Page<Following> followings = new PageImpl<>(expectedFollowings, PageRequest.of(0, 3),
                expectedFollowings.size());

        // given
        given(followRepository.findFollowers(criteria2, followers.getPageable())).willReturn(followers);
        given(followRepository.findFollowed(any(), any())).willReturn(followings);

        Page<JsonPerson> actualBySubscribedToAndDisplayName = followManager.findFollowers(criteria2, followers.getPageable());

        assertTrue(actualBySubscribedToAndDisplayName.isFirst());
        assertEquals(1, actualBySubscribedToAndDisplayName.getTotalPages());
        assertEquals(expectedFollowers.size(), actualBySubscribedToAndDisplayName.getContent().size());
        assertEquals(expectedFollowers.size(), actualBySubscribedToAndDisplayName.getTotalElements());

        actualBySubscribedToAndDisplayName.forEach(p -> {
            if (2L == p.getId()) {
                assertTrue(p.isFollowing());
            } else {
                assertFalse(p.isFollowing());
            }
        });
    }

    @Test
    void findFollowedPersonsOld_bySearchCriteria_returnPaginatedResult() {
        // 1. Find by subscriber:
        FollowSearchCriteria criteria1 = FollowSearchCriteria.builder()
                .personId(1L)
                .build();

        List<Following> expectedBySubscriber = getFollowings().stream()
                .filter(p -> p.getSubscriber().getId().equals(criteria1.getPersonId())).collect(Collectors.toList());
        Page<Following> page1 = new PageImpl<>(expectedBySubscriber, PageRequest.of(0, 3), expectedBySubscriber.size());

        // given
        given(followRepository.findFollowed(criteria1, page1.getPageable())).willReturn(page1);

        Page<Following> actualBySubscriber = followManager.findFollowedPersonsOld(criteria1, page1.getPageable());

        assertTrue(actualBySubscriber.isFirst());
        assertEquals(2, actualBySubscriber.getTotalPages());
        assertEquals(expectedBySubscriber.size(), actualBySubscriber.getContent().size());
        assertEquals(expectedBySubscriber.size(), actualBySubscriber.getTotalElements());

        // 2. Find by subscriber and subscribedTo display name:
        FollowSearchCriteria criteria2 = FollowSearchCriteria.builder()
                .personId(1L)
                .query(USER_2)
                .build();

        List<Following> expectedBySubscriberAndToAndDisplayName = getFollowings().stream()
                .filter(p -> p.getSubscriber().getId().equals(criteria2.getPersonId()) &&
                        p.getSubscribedTo().getDisplayName().contains(criteria2.getQuery())).collect(Collectors.toList());
        Page<Following> page2 = new PageImpl<>(expectedBySubscriberAndToAndDisplayName, PageRequest.of(0, 3),
                expectedBySubscriberAndToAndDisplayName.size());

        // given
        given(followRepository.findFollowed(criteria2, page2.getPageable())).willReturn(page2);

        Page<Following> actualBySubscriberAndDisplayName = followManager.findFollowedPersonsOld(criteria2, page2.getPageable());

        assertTrue(actualBySubscriberAndDisplayName.isFirst());
        assertEquals(1, actualBySubscriberAndDisplayName.getTotalPages());
        assertEquals(expectedBySubscriberAndToAndDisplayName.size(), actualBySubscriberAndDisplayName.getContent().size());
        assertEquals(expectedBySubscriberAndToAndDisplayName.size(), actualBySubscriberAndDisplayName.getTotalElements());
    }

    @Test
    void findFollowedPersons_bySearchCriteria_returnPaginatedResult() {
        // given
        FollowSearchCriteria criteria1 = FollowSearchCriteria.builder()
                .personId(1L)
                .build();

        List<Following> expectedBySubscriber = getFollowings().stream()
                .filter(p -> p.getSubscriber().getId().equals(criteria1.getPersonId())).collect(Collectors.toList());
        Page<Following> page1 = new PageImpl<>(expectedBySubscriber, PageRequest.of(0, 3), expectedBySubscriber.size());
        List<Long> followings = expectedBySubscriber.stream().map(following -> following.getSubscriber().getId()).collect(Collectors.toList());

        List<Following> followedWhoFollowBackIds = getFollowings().stream()
                .filter(p -> p.getSubscribedTo().getId().equals(criteria1.getPersonId()) && followings.contains(p.getId()))
                .collect(Collectors.toList());
        Page<Following> page2 = new PageImpl<>(followedWhoFollowBackIds, PageRequest.of(0, 3), followedWhoFollowBackIds.size());

        given(followRepository.findFollowed(criteria1, page1.getPageable())).willReturn(page1);
        given(followRepository.findFollowers(any(), any())).willReturn(page2);

        //when
        Page<JsonPerson> actualFollowedPersons = followManager.findFollowedPersons(criteria1, page1.getPageable());

        //then
        assertEquals(2, actualFollowedPersons.getTotalPages());
        assertEquals(expectedBySubscriber.size(), actualFollowedPersons.getContent().size());
        assertEquals(expectedBySubscriber.size(), actualFollowedPersons.getTotalElements());
        assertEquals(1, actualFollowedPersons.getContent().stream().filter(JsonPerson::isFollowing).count());
        assertEquals(3, actualFollowedPersons.getContent().stream().filter(jsonPerson -> !jsonPerson.isFollowing()).count());
    }

    @NonNull
    private List<Following> getFollowings() {
        Following person1 = Following.builder()
                .id(1L)
                .subscriber(Person.builder().id(2L).displayName(USER_2).personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .subscribedTo(Person.builder().id(1L).displayName(USER_1).personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .build();
        Following person2 = Following.builder()
                .id(2L)
                .subscriber(Person.builder().id(3L).displayName("User 3").personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .subscribedTo(Person.builder().id(1L).displayName(USER_1).personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .build();
        Following person3 = Following.builder()
                .id(3L)
                .subscriber(Person.builder().id(4L).displayName("User 4").personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .subscribedTo(Person.builder().id(1L).displayName(USER_1).personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .build();
        Following person4 = Following.builder()
                .id(4L)
                .subscriber(Person.builder().id(5L).displayName("User 5").personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .subscribedTo(Person.builder().id(1L).displayName(USER_1).personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .build();
        Following person5 = Following.builder()
                .id(5L)
                .subscriber(Person.builder().id(1L).displayName(USER_1).personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .subscribedTo(Person.builder().id(2L).displayName(USER_2).personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .build();
        Following person6 = Following.builder()
                .id(6L)
                .subscriber(Person.builder().id(1L).displayName(USER_1).personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .subscribedTo(Person.builder().id(7L).displayName("User 7").personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .build();
        Following person7 = Following.builder()
                .id(7L)
                .subscriber(Person.builder().id(1L).displayName(USER_1).personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .subscribedTo(Person.builder().id(8L).displayName("User 8").personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .build();
        Following person8 = Following.builder()
                .id(8L)
                .subscriber(Person.builder().id(1L).displayName(USER_1).personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .subscribedTo(Person.builder().id(9L).displayName("User 9").personRoles(Collections.emptySet()).personAuthorities(Collections.emptySet()).build())
                .build();

        List<Following> personList = new ArrayList<>();
        personList.add(person1);
        personList.add(person2);
        personList.add(person3);
        personList.add(person4);
        personList.add(person5);
        personList.add(person6);
        personList.add(person7);
        personList.add(person8);
        return personList;
    }

}
