package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.FollowSearchCriteria;
import iq.earthlink.social.personservice.person.model.Following;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Following, Long> {

    @Query("DELETE FROM Following f WHERE f.subscriber.id = ?1 AND f.subscribedTo.id = ?2")
    @Modifying
    void unfollow(Long followerId, Long followedId);

    @Query("SELECT f FROM Following f "
            + "WHERE (:#{#criteria.personId} IS NULL OR f.subscribedTo.id = :#{#criteria.personId}) "
            + "AND (:#{#criteria.followerIds} IS NULL OR f.subscriber.id IN :#{#criteria.followerIds}) "
            + "AND (:#{#criteria.query} IS NULL "
            + "   OR LOWER(f.subscriber.firstName) LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%') "
            + "   OR LOWER(f.subscriber.lastName) LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%') "
            + "   OR LOWER(f.subscriber.displayName) LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%'))")
    Page<Following> findFollowers(@Param("criteria") FollowSearchCriteria criteria, Pageable page);

    @Query("SELECT f FROM Following f "
            + "WHERE (:#{#criteria.personId} IS NULL OR f.subscriber.id = :#{#criteria.personId}) "
            + "AND (:#{#criteria.followingIds} IS NULL OR f.subscribedTo.id IN :#{#criteria.followingIds}) "
            + "AND (:#{#criteria.query} IS NULL "
            + "   OR LOWER(f.subscribedTo.firstName) LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%') "
            + "   OR LOWER(f.subscribedTo.lastName) LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%') "
            + "   OR LOWER(f.subscribedTo.displayName) LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%'))")
    Page<Following> findFollowed(@Param("criteria") FollowSearchCriteria criteria, Pageable page);

    @Query("SELECT count(f) from Following f where f.subscribedTo.id=?1")
    long getFollowersCount(Long personId);

    @Query("SELECT count(f) from Following f where f.subscriber.id=?1")
    long getFollowedCount(Long personId);

    Page<Following> findFollowingBySubscribedToIdAndSubscriberIdIn(Long personId, List<Long> personIds, Pageable pageable);

    @Query("SELECT f.subscriber.id FROM Following f WHERE f.subscribedTo.id = :subscribedToId")
    List<Long> getFollowerIds(@Param("subscribedToId") Long subscribedToId);

    @Query("SELECT f.subscribedTo.id FROM Following f WHERE f.subscriber.id = :subscriberId")
    List<Long> getSubscribedToIds(@Param("subscriberId") Long subscriberId);
}
