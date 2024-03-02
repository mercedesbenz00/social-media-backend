package iq.earthlink.social.groupservice.config;

import iq.earthlink.social.classes.enumeration.GroupEventType;
import iq.earthlink.social.common.data.event.GroupActivityEvent;
import iq.earthlink.social.groupservice.group.GroupRepository;
import iq.earthlink.social.groupservice.group.UserGroup;
import iq.earthlink.social.groupservice.group.UserGroupStats;
import iq.earthlink.social.groupservice.group.member.GroupMember;
import iq.earthlink.social.groupservice.group.member.GroupMemberRepository;
import iq.earthlink.social.groupservice.group.rest.enumeration.ApprovalState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CommandLineAppStartupRunner implements CommandLineRunner {
    GroupRepository groupRepository;
    GroupMemberRepository groupMemberRepository;
    RabbitTemplate rabbitTemplate;

    public CommandLineAppStartupRunner(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
                                       RabbitTemplate rabbitTemplate) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("Running app startup configurations");

        // Update members count in group statistics if it's not up-to-date:
        List<UserGroup> groups = groupRepository.findAll();
        List<GroupMember> groupMembers = groupMemberRepository.findByStateAndGroupIn(ApprovalState.APPROVED, groups);
        Map<Long, Long> groupMembersCountMap = groupMembers.stream()
                .collect(Collectors.groupingBy(gm -> gm.getGroup().getId(), Collectors.counting()));

        groups.forEach(group -> {
            UserGroupStats stats = group.getStats();
            long groupMembersCount = groupMembersCountMap.containsKey(group.getId()) ? groupMembersCountMap.get(group.getId()) : 0;
            long delta = groupMembersCount - stats.getMembersCount();
            if (delta != 0) {
                // update group statistics:
                GroupActivityEvent.send(rabbitTemplate,
                        new GroupActivityEvent(group.getId(), delta > 0 ?
                                GroupEventType.MEMBER_JOINED.name() :
                                GroupEventType.MEMBER_LEFT.name(),
                                Math.abs(delta)));
            }
        });
    }
}
