package iq.earthlink.social.postservice.group.member.repository;

import iq.earthlink.social.postservice.group.member.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    @Query("SELECT g from GroupMember g where g.userGroup.groupId = :groupId and g.person.personId = :personId")
    Optional<GroupMember> getByGroupIdAndPersonId(@Param("groupId") Long groupId, @Param("personId") Long personId);

    @Modifying
    @Query("UPDATE GroupMember g SET g.permissions = :permissions where g.id = :id")
    void setGroupMemberPermissions(@Param("permissions") String permissions, @Param("id") Long id);

    @Query("SELECT g.person.personId from GroupMember g " +
            "WHERE g.userGroup.groupId = :groupId")
    Set<Long> getAllMemberIdsByGroupId(@Param("groupId") Long groupId);

    @Query(value = "select * from group_member gm " +
            "join person p on gm.person_id=p.id " +
            "join user_group us on gm.group_id=us.id " +
            "WHERE us.group_id = ?1 " +
            "AND permissions @> cast(?2 as jsonb)", nativeQuery = true)
    List<GroupMember> findByGroupIdAndPermissions(Long groupId, String permissions);

    @Query(value = "select * from group_member gm " +
            "join person p on gm.person_id=p.id " +
            "join user_group us on gm.group_id=us.id " +
            "WHERE p.person_id = ?1 " +
            "AND permissions @> cast(?2 as jsonb)", nativeQuery = true)
    List<GroupMember> findByPersonIdAndPermissions(Long personId, String permissions);
}
