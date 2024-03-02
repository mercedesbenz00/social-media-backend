package iq.earthlink.social.groupservice.category;

import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.groupservice.category.dto.CategoryRequest;
import iq.earthlink.social.groupservice.category.dto.CategoryStats;
import iq.earthlink.social.groupservice.group.CategorySearchCriteria;
import iq.earthlink.social.groupservice.group.rest.JsonCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryManager {

    Category getCategory(Long categoryId);

    List<Category> getCategories(Iterable<Long> ids);

    Page<JsonCategory> findCategories(CategorySearchCriteria categorySearchCriteria, String authorizationHeader, Pageable page);

    Page<CategoryModel> findCategoriesInternal(CategorySearchCriteria categorySearchCriteria, Pageable page);

    Page<CategoryModel> searchCategories(CategorySearchCriteria criteria, Pageable page);

    Category createCategory(String authorizationHeader, CategoryRequest categoryRequest);

    Category updateCategory(Long categoryId, CategoryRequest categoryRequest, boolean isAdmin);

    void removeCategory(Long categoryId, boolean isAdmin);

    PersonCategory addPersonCategory(Long categoryId, Long personId);

    void removePersonCategory(Long categoryId, Long personId);

    PersonCategory addPersonCategories(List<Long> categoryIds, Long personId);

    PersonCategory initPersonCategories(String authorizationHeader, List<Long> categoryIds, Long personId);

    void removePersonCategories(Long personId);

    CategoryStats getCategoryStats(String fromDate, TimeInterval timeInterval);
}
