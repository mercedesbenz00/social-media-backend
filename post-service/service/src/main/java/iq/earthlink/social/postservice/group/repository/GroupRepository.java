package iq.earthlink.social.postservice.group.repository;

import iq.earthlink.social.postservice.group.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<UserGroup, Long> {
    Optional<UserGroup> getByGroupId(Long groupId);

    List<UserGroup> getByGroupIdIn(List<Long> groupIds);

    @Query("SELECT ug.groupId "
            + "FROM GroupMember gm "
            + "JOIN UserGroup ug ON gm.userGroup.id = ug.id "
            + "WHERE gm.person.personId = :personId "
            + "AND (:groupIds IS NULL OR ug.groupId IN :groupIds)")
    List<Long> getMyGroupIds(@Param("personId") Long personId, @Param("groupIds") List<Long> groupIds);

    @Query("SELECT ug.groupId FROM UserGroup ug "
            + "WHERE (ug.groupId IN (SELECT gm.userGroup.groupId "
            +                           "FROM GroupMember gm "
            +                           "WHERE gm.person.personId = :personId) "
            +       "OR ug.accessType='PUBLIC') "
            + "AND ug.groupId = :groupId")
    Optional<Long> getAccessibleGroup(@Param("personId") Long personId, @Param("groupId") Long groupId);
}
