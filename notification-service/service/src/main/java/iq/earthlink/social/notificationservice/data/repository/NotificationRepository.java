package iq.earthlink.social.notificationservice.data.repository;

import iq.earthlink.social.classes.enumeration.NotificationState;
import iq.earthlink.social.notificationservice.data.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByIdIn(List<Long> ids);

    Page<Notification> findByReceiverIdAndCreatedDateAfterOrderByCreatedDateDesc(Long receiverPersonId, Date afterDate, Pageable pageable);
    @Modifying
    @Query(value = "DELETE FROM Notification n " +
            "WHERE n.receiver_id  in :receiverIds " +
            "AND n.meta ->>'groupId' = :groupId " +
            "AND n.state = 'NEW' " +
            "AND n.topic = 'USER_INVITED_TO_GROUP'", nativeQuery = true)
    void deletePreviousGroupInvitations(@Param("receiverIds") List<Long> receiverIds, @Param("groupId") String groupId);

    @Query("SELECT n FROM Notification n " +
            "WHERE n.createdDate >= :date " +
            "AND n.receiverId  = :receiverId " +
            "AND n.authorId IS NOT NULL " +
            "AND n.state <> 'DELETED' " +
            "ORDER BY n.createdDate DESC")
    Page<Notification> findActiveNotifications(@Param("receiverId") Long receiverId, @Param("date") Date afterDate, Pageable pageable);

    @Modifying
    @Query("delete from Notification n where n.createdDate < ?1")
    @Transactional
    int removeByCreatedDateBefore(Date date);

    Set<Notification> findByBatchIdIn(Set<Integer> batchId);

    Page<Notification> findByReceiverIdAndStateAndCreatedDateAfterOrderByCreatedDateDesc(Long receiverPersonId, NotificationState state, Date afterDate, Pageable pageable);
}
