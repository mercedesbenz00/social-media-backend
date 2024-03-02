package iq.earthlink.social.postservice.story;

import iq.earthlink.social.postservice.story.model.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Set;

public interface StoryRepository extends JpaRepository<Story, Long> {

    @Query("SELECT s from Story s " +
            "LEFT JOIN s.selectedFollowerIds f " +
            "LEFT JOIN s.views sv " +
            "WHERE ((s.accessType = 'SELECTED_FOLLOWERS' AND (f = :personId OR s.authorId = :personId)) " +
            "OR (s.accessType = 'ALL_FOLLOWERS' AND (s.authorId IN :subscriptionIds OR s.authorId = :personId)) " +
            "OR s.accessType = 'PUBLIC') " +
            "AND (coalesce(:authorIds, null) IS NULL OR s.authorId IN :authorIds) " +
            "AND s.createdAt >= :createdAt " +
            "GROUP BY sv.story.id, s.id, s.createdAt " +
            "ORDER BY sv.story.id DESC, " +
            "   CASE s.accessType " +
            "      WHEN 'PUBLIC' THEN 2 " +
            "      ELSE 1 " +
            "   END, s.createdAt DESC")
    Page<Story> findAllowedStoriesAndSortUnseenFirst(@Param("personId") Long personId, @Param("authorIds") Set<Long> authorIds,
                                                     @Param("subscriptionIds") List<Long> subscriptionIds,
                                                     @Param("createdAt") Date createdAt, Pageable page);

    @Query("SELECT s from Story s " +
            "LEFT JOIN s.selectedFollowerIds f " +
            "LEFT JOIN s.views sv " +
            "WHERE ((s.accessType = 'SELECTED_FOLLOWERS' AND (f = :personId OR s.authorId = :personId)) " +
            "OR (s.accessType = 'ALL_FOLLOWERS' AND (s.authorId IN :subscriptionIds OR s.authorId = :personId)) " +
            "OR s.accessType = 'PUBLIC') " +
            "AND (coalesce(:authorIds, null) IS NULL OR s.authorId IN :authorIds) " +
            "AND s.createdAt >= :createdAt " +
            "AND sv IS NULL " +
            "ORDER BY " +
            "   CASE s.accessType " +
            "      WHEN 'PUBLIC' THEN 2 " +
            "      ELSE 1 " +
            "   END, s.createdAt DESC")
    Page<Story> findAllowedUnseenStoriesAndSort(@Param("personId") Long personId, @Param("authorIds") Set<Long> authorIds,
                                                @Param("subscriptionIds") List<Long> subscriptionIds,
                                                @Param("createdAt") Date createdAt, Pageable page);
}