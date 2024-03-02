package iq.earthlink.social.groupservice.category.repository;

import iq.earthlink.social.groupservice.category.Category;
import iq.earthlink.social.groupservice.category.CategoryModel;
import iq.earthlink.social.groupservice.category.PersonCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonCategoryRepository extends JpaRepository<PersonCategory, Long> {

    @Query("SELECT new iq.earthlink.social.groupservice.category.CategoryModel(c.id as id, coalesce(l.name, c.name) as name, "
            + "c.createdAt as createdAt, c.categoryUUID, c.parentCategory as parentCategory, c.avatar, c.cover, c.personCount, c.groupCount) "
            + "FROM PersonCategory pc "
            + "JOIN pc.categories AS c "
            + "LEFT JOIN c.parentCategory AS prc "
            + "LEFT JOIN c.avatar AS av "
            + "LEFT JOIN c.cover AS cv "
            + "LEFT JOIN c.localizations AS l on LOWER(l.locale)=:locale "
            + "WHERE (:personId IS NULL OR pc.personId = :personId) "
            + "AND (:parentCatId IS NULL OR c.parentCategory.id = :parentCatId)")
    Page<CategoryModel> findCategories(
            @Param("personId") Long personId,
            @Param("parentCatId") Long parentCategoryId,
            @Param("locale") String locale,
            Pageable page);

    @Query("SELECT pc FROM PersonCategory pc WHERE pc.personId = ?1")
    Optional<PersonCategory> findPersonCategory(Long personId);

    @Query(value = "DELETE FROM person_category_categories c WHERE c.categories_id = ?1", nativeQuery = true)
    @Modifying
    void deleteCategory(Long categoryId);

    @Query("DELETE FROM PersonCategory pc WHERE pc.personId = :personId")
    @Modifying
    void removePersonCategories(Long personId);

    Page<PersonCategory> getPersonCategoriesByCategoriesIn(List<Category> categories, Pageable page);

}
