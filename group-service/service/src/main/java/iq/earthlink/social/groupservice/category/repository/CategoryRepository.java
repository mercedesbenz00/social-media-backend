package iq.earthlink.social.groupservice.category.repository;

import iq.earthlink.social.groupservice.category.Category;
import iq.earthlink.social.groupservice.category.CategoryModel;
import iq.earthlink.social.groupservice.category.dto.CreatedCategories;
import iq.earthlink.social.groupservice.group.CategorySearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT new iq.earthlink.social.groupservice.category.CategoryModel(c.id as id, coalesce(l.name, c.name) as name, " +
            "c.createdAt as createdAt, c.categoryUUID, c.parentCategory, c.avatar, c.cover, c.personCount, c.groupCount)  " +
            "FROM Category c " +
            "LEFT JOIN c.localizations l on LOWER(l.locale)=:#{#criteria.locale} " +
            "LEFT JOIN c.parentCategory pc " +
            "LEFT JOIN c.avatar av " +
            "LEFT JOIN c.cover cv " +
            "WHERE " +
            " ((:#{#criteria.parentCategoryId} IS NULL AND c.parentCategory IS NULL) OR (c.parentCategory.id = :#{#criteria.parentCategoryId})) "
            + "AND (:#{#criteria.query} IS NULL OR (LOWER(coalesce(l.name, c.name)) LIKE CONCAT('%', LOWER(?#{#criteria.query ?:''}), '%'))) "
            + "AND ((:#{#criteria.skipUnusedInGroups} = false ) OR  (c.groupCount > 0) )  ")
    Page<CategoryModel> findCategories(@Param("criteria") CategorySearchCriteria criteria, Pageable page);

    @Query("SELECT new iq.earthlink.social.groupservice.category.CategoryModel(c.id as id, coalesce(l.name, c.name) as name, " +
            "c.createdAt as createdAt, c.categoryUUID, c.parentCategory, c.avatar, c.cover, c.personCount, c.groupCount)  " +
            "FROM Category c " +
            "LEFT JOIN c.localizations l on LOWER(l.locale)=:#{#criteria.locale} " +
            "LEFT JOIN c.parentCategory pc " +
            "LEFT JOIN c.avatar av " +
            "LEFT JOIN c.cover cv " +
            "WHERE " +
            " ( ( :#{#criteria.skipParent} is false ) OR (c.parentCategory IS NOT NULL) )"
            +" AND (:#{#criteria.query} = '%' OR LOWER(coalesce(l.name, c.name)) LIKE :#{#criteria.query} "
            +"OR SIMILARITY(LOWER(coalesce(l.name, c.name)), :#{#criteria.query}) > :#{#criteria.similarityThreshold})")
    Page<CategoryModel> searchCategories(@Param("criteria") CategorySearchCriteria criteria, Pageable page);

    @Query("SELECT new iq.earthlink.social.groupservice.category.CategoryModel(c.parentCategory as parentCategory, c.id as id, " +
            "count(ug.id) as groupCount) " +
            "FROM UserGroup ug  " +
            "LEFT JOIN ug.categories ugc " +
            "LEFT JOIN Category c on ugc.id = c.id " +
            "LEFT JOIN c.parentCategory pp on pp.id = c.parentCategory.id " +
            "WHERE ug.state = 'APPROVED' " +
            "GROUP BY c.id, c.parentCategory")
    List<CategoryModel> findGroupCountForCategories();

    @Query("SELECT new iq.earthlink.social.groupservice.category.CategoryModel(pcc.id as id, count(pcc.id) as personCount, " +
            "c.parentCategory as parentCategory) " +
            "FROM PersonCategory pc  " +
            "LEFT JOIN pc.categories pcc " +
            "JOIN Category c on pcc.id = c.id " +
            "LEFT JOIN c.parentCategory pp on pp.id = c.parentCategory.id " +
            "GROUP BY pcc.id, c.parentCategory")
    List<CategoryModel> findPersonCountForCategories();

    @Query("SELECT count(c.id) from Category c ")
    long getAllCategoriesCount();

    @Query("SELECT count(c.id) from Category c WHERE coalesce(:fromDate, NULL) IS NULL OR c.createdAt >= :fromDate")
    long getNewCategoriesCount(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.groupservice.category.dto.CreatedCategories(to_char(c.createdAt, 'YYYY-MM') AS date, count(c.id)) " +
            "FROM Category c " +
            "WHERE coalesce(:fromDate, NULL) IS NULL OR c.createdAt >= :fromDate " +
            "GROUP BY date ")
    List<CreatedCategories> getCreatedCategoriesPerMonth(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.groupservice.category.dto.CreatedCategories(to_char(c.createdAt, 'YYYY-MM-dd') AS date, count(c.id)) " +
            "FROM Category c " +
            "WHERE coalesce(:fromDate, NULL) IS NULL OR c.createdAt >= :fromDate " +
            "GROUP BY date ")
    List<CreatedCategories> getCreatedCategoriesPerDay(@Param("fromDate") Date fromDate);

    @Query("SELECT new iq.earthlink.social.groupservice.category.dto.CreatedCategories(to_char(c.createdAt, 'YYYY') AS date, count(c.id)) " +
            "FROM Category c " +
            "WHERE coalesce(:fromDate, NULL) IS NULL OR c.createdAt >= :fromDate " +
            "GROUP BY date ")
    List<CreatedCategories> getCreatedCategoriesPerYear(@Param("fromDate") Date fromDate);
}
