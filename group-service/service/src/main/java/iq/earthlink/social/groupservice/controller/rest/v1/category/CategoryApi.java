package iq.earthlink.social.groupservice.controller.rest.v1.category;

import io.swagger.annotations.*;
import iq.earthlink.social.exception.RestApiException;
import iq.earthlink.social.groupservice.category.Category;
import iq.earthlink.social.groupservice.category.CategoryManager;
import iq.earthlink.social.groupservice.category.CategoryModel;
import iq.earthlink.social.groupservice.category.dto.CategoryRequest;
import iq.earthlink.social.groupservice.group.CategorySearchCriteria;
import iq.earthlink.social.groupservice.group.rest.JsonCategory;
import iq.earthlink.social.groupservice.group.rest.JsonCategoryWithLocalization;
import iq.earthlink.social.groupservice.group.rest.JsonPersonCategory;
import iq.earthlink.social.groupservice.util.RoleUtil;
import iq.earthlink.social.security.DefaultSecurityProvider;
import lombok.extern.slf4j.Slf4j;
import org.dozer.Mapper;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Locale;

@Api(tags = "Category Api", value = "CategoryApi")
@RestController
@RequestMapping(value = "/api/v1/categories", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class CategoryApi {

    private final CategoryManager categoryManager;
    private final Mapper mapper;
    private final DefaultSecurityProvider securityProvider;
    private final RoleUtil roleUtil;

    public CategoryApi(
            CategoryManager categoryManager,
            Mapper mapper,
            DefaultSecurityProvider securityProvider, RoleUtil roleUtil) {
        this.categoryManager = categoryManager;
        this.mapper = mapper;
        this.securityProvider = securityProvider;
        this.roleUtil = roleUtil;
    }

    @GetMapping
    @ApiOperation("Find categories by the criteria")
    public Page<JsonCategory> getCategories(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Filters categories interested by the person")
            @RequestParam(required = false) Long personId,
            @ApiParam("Filters categories that have parent category. If not specified - returns only root categories.")
            @RequestParam(required = false) Long parentCategoryId,
            @ApiParam("Filters categories if the name matched by the query")
            @RequestParam(required = false) String query,
            @ApiParam("Filters categories if it is used in groups")
            @RequestParam(required = false) boolean skipUnusedInGroups,
            Pageable pageable) {

        CategorySearchCriteria categorySearchCriteria = CategorySearchCriteria.builder()
                .query(query)
                .personId(personId)
                .locale(LocaleContextHolder.getLocale().getLanguage())
                .parentCategoryId(parentCategoryId)
                .skipUnusedInGroups(skipUnusedInGroups)
                .build();

        return categoryManager.findCategories(categorySearchCriteria, authorizationHeader, pageable);
    }

    @GetMapping("/search")
    @ApiOperation("Search through all categories")
    public Page<JsonCategory> searchCategories(
            @ApiParam("Filters categories if the name matched by the query")
            @RequestParam(required = false) String query,
            @ApiParam("If true returns only not root categories")
            @RequestParam(required = false) boolean skipParent,
            Pageable pageable) {

        CategorySearchCriteria criteria = CategorySearchCriteria.builder()
                .query(query)
                .skipParent(skipParent)
                .locale(LocaleContextHolder.getLocale().getLanguage())
                .build();

        return categoryManager.searchCategories(criteria, pageable)
                .map(c -> mapper.map(c, JsonCategory.class));
    }

    @PostMapping
    @ApiOperation("Creates new category")
    public JsonCategoryWithLocalization createCategory(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid CategoryRequest categoryRequest) {
        LocaleContextHolder.setLocale(Locale.getDefault());

        return mapper.map(categoryManager.createCategory(authorizationHeader, categoryRequest), JsonCategoryWithLocalization.class);
    }

    @PutMapping("/{categoryId}")
    @ApiOperation("Update category by id")
    @ApiResponses(
            @ApiResponse(message = "Category successfully updated", code = 200, response = Category.class)
    )
    public JsonCategoryWithLocalization updateCategory(@RequestHeader("Authorization") String authorizationHeader,
                                                       @PathVariable("categoryId") Long categoryId,
                                                       @RequestBody @Valid CategoryRequest categoryRequest) {
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);

        return mapper.map(categoryManager.updateCategory(categoryId, categoryRequest, isAdmin), JsonCategoryWithLocalization.class);
    }

    @DeleteMapping(value = "/{categoryId}")
    @ApiOperation("Removes category by id")
    @ApiResponses(
            @ApiResponse(message = "Category successfully removed", code = 200)
    )
    public void deleteCategory(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long categoryId) {
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        try {
            categoryManager.removeCategory(categoryId, isAdmin);
        } catch (DataIntegrityViolationException ex) {
            throw new RestApiException(HttpStatus.CONFLICT, "error.category.has.child.categories", categoryId);
        }
    }

    @GetMapping("/{categoryId}")
    @ApiOperation("Get category by id")
    @ApiResponses(
            @ApiResponse(message = "Get Category", code = 200)
    )
    public JsonCategory getCategory(
            @PathVariable Long categoryId) {

        CategoryModel category = mapper.map(categoryManager.getCategory(categoryId), CategoryModel.class);
        return mapper.map(category, JsonCategory.class);
    }

    /**
     * @deprecated (Using in the ios and android apps)
     */
    @ApiOperation("Add category by id to the current user interests")
    @PutMapping("/{categoryId}/persons")
    @Deprecated(forRemoval = true)
    public JsonPersonCategory addPersonCategory(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long categoryId) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return mapper.map(categoryManager.addPersonCategory(categoryId, personId), JsonPersonCategory.class);
    }

    /**
     * @deprecated (Using in the ios and android apps)
     */
    @ApiOperation("Removes the category from the current user interests")
    @DeleteMapping("/{categoryId}/persons")
    @Deprecated(forRemoval = true)
    public void removePersonCategory(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long categoryId) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        categoryManager.removePersonCategory(categoryId, personId);
    }

    @ApiOperation("Assign categories to the current user")
    @PutMapping("/persons")
    public JsonPersonCategory addPersonCategories(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam List<Long> categoryIds,
            @RequestParam(required = false) boolean onboarding) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        return mapper.map(onboarding ?
                        categoryManager.initPersonCategories(authorizationHeader, categoryIds, personId) :
                        categoryManager.addPersonCategories(categoryIds, personId),
                JsonPersonCategory.class);
    }

}
