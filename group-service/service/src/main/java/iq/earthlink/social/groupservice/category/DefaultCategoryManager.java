package iq.earthlink.social.groupservice.category;

import iq.earthlink.social.classes.enumeration.TimeInterval;
import iq.earthlink.social.common.audit.AuditMarker;
import iq.earthlink.social.common.audit.AuditMessage;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.exception.*;
import iq.earthlink.social.groupservice.category.dto.CategoryRequest;
import iq.earthlink.social.groupservice.category.dto.CategoryStats;
import iq.earthlink.social.groupservice.category.dto.CreatedCategories;
import iq.earthlink.social.groupservice.category.repository.CategoryRepository;
import iq.earthlink.social.groupservice.category.repository.PersonCategoryRepository;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import iq.earthlink.social.groupservice.group.CategorySearchCriteria;
import iq.earthlink.social.groupservice.group.dto.OnboardDTO;
import iq.earthlink.social.groupservice.group.dto.OnboardState;
import iq.earthlink.social.groupservice.group.rest.JsonCategory;
import iq.earthlink.social.groupservice.util.RoleUtil;
import iq.earthlink.social.security.DefaultSecurityProvider;
import iq.earthlink.social.util.LocalizationUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;

@Service
@RequiredArgsConstructor
public class DefaultCategoryManager implements CategoryManager {
    public static final Logger LOGGER = LogManager.getLogger(DefaultCategoryManager.class);

    private final CategoryRepository repository;
    private final PersonCategoryRepository personCategoryRepository;
    private final CategoryMediaService mediaService;
    private final DefaultSecurityProvider securityProvider;
    private final KafkaProducerService kafkaProducerService;
    private final RoleUtil roleUtil;
    private final Mapper mapper;

    @Override
    public Category getCategory(Long categoryId) {
        return repository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("error.category.not.found", categoryId));
    }

    @Override
    public List<Category> getCategories(Iterable<Long> ids) {
        if (!ids.iterator().hasNext()) {
            return Collections.emptyList();
        }

        return repository.findAllById(ids);
    }

    @Override
    public Page<JsonCategory> findCategories(CategorySearchCriteria categorySearchCriteria, String authorizationHeader, Pageable page) {

        if (categorySearchCriteria.getPersonId() != null) {
            Long callerPersonId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
            String[] callerRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
            boolean isCallerAdmin = roleUtil.isAdmin(callerRoles);

            if (!(Objects.equals(callerPersonId, categorySearchCriteria.getPersonId()) || isCallerAdmin))
                throw new ForbiddenException("error.person.not.authorized");
        }

        return findCategoriesInternal(categorySearchCriteria, page)
                .map(c -> mapper.map(c, JsonCategory.class));
    }

    @Override
    public Page<CategoryModel> findCategoriesInternal(CategorySearchCriteria categorySearchCriteria, Pageable page) {
        Page<CategoryModel> categories;
        if (categorySearchCriteria.getPersonId() == null) {
            categories = repository.findCategories(categorySearchCriteria, page);
        } else {
            categories = personCategoryRepository.findCategories(categorySearchCriteria.getPersonId(),
                    categorySearchCriteria.getParentCategoryId(), categorySearchCriteria.getLocale(), page);
        }

        return categories;
    }

    @Override
    public Page<CategoryModel> searchCategories(CategorySearchCriteria criteria, Pageable page) {
        updateQuery(criteria);
        return repository.searchCategories(criteria, page);
    }

    @Transactional
    @Override
    public Category createCategory(String authorizationHeader, CategoryRequest req) {
        String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
        boolean isAdmin = roleUtil.isAdmin(personRoles);
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        if (!isAdmin) throw new ForbiddenException("error.person.unauthorized.to.create.category");

        Category category = new Category();
        category.setName(req.getName());
        setCategoryLocalizations(req, category);
        setParentCategoryId(req, category);
        try {
            Category saved = repository.saveAndFlush(category);

            String logMessage = String.format("Successfully created category: \"%s\"", saved.getName());
            LOGGER.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.CREATE_CATEGORY, logMessage, personId, saved.getId()));

            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new NotUniqueException("error.category.create.duplicate", req.getName(), ex.getMostSpecificCause().getMessage());
        }
    }

    @Transactional
    @Override
    public Category updateCategory(Long categoryId, CategoryRequest req, boolean isAdmin) {

        if (!isAdmin) throw new ForbiddenException("error.person.unauthorized.to.update.category");

        Category category = getCategory(categoryId);
        try {
            if (req.getName() != null) {
                category.setName(req.getName());
            }
            setCategoryLocalizations(req, category);
            setParentCategoryId(req, category);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new RestApiException(HttpStatus.CONFLICT, "error.category.update.with.existing.name", categoryId);
        }
        return category;
    }

    @Transactional
    public void setCategoryLocalizations(CategoryRequest req, Category c) {
        if (req.getLocalizations() != null && !req.getLocalizations().isEmpty()) {
            c.getLocalizations().clear();
            repository.flush();
            for (Map.Entry<String, String> entry : req.getLocalizations().entrySet()) {
                String locale = entry.getKey();
                String name = entry.getValue();
                LocalizationUtil.checkLocalization(locale);
                c.getLocalizations().put(locale,
                        createCategoryLocalized(c, name, locale));
            }
        }
    }

    @Transactional
    public void setParentCategoryId(CategoryRequest req, Category c) {
        Long parentCategoryId = req.getParentCategoryId();
        if (parentCategoryId != null) {
            if (!parentCategoryId.equals(c.getId())) {
                Category parent = getCategory(parentCategoryId);
                c.setParentCategory(parent);
            }
        } else {
            c.setParentCategory(null);
        }
    }

    @Transactional
    @Override
    public void removeCategory(Long categoryId, boolean isAdmin) {
        if (!isAdmin) throw new ForbiddenException("error.person.unauthorized.to.remove.category");

        if (repository.existsById(categoryId)) {
            Category category = repository.getReferenceById(categoryId);
            Page<PersonCategory> personCategories = personCategoryRepository.getPersonCategoriesByCategoriesIn(new ArrayList<>(Collections.singletonList(category)), Pageable.unpaged());
            personCategoryRepository.deleteAll(personCategories);
            removeMediaFiles(category, true);
            repository.deleteById(categoryId);
        }
    }

    @Transactional
    @Override
    public PersonCategory addPersonCategory(@Nonnull Long categoryId, @Nonnull Long personId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, "personId");
        checkNotNull(categoryId, ERROR_CHECK_NOT_NULL, "categoryId");

        Category category = getCategory(categoryId);
        checkIfAssignable(List.of(category));

        PersonCategory pc = personCategoryRepository
                .findPersonCategory(personId)
                .orElseGet(PersonCategory::new);

        pc.setPersonId(personId);
        pc.getCategories().add(category);

        return personCategoryRepository.save(pc);
    }

    @Transactional
    @Override
    public void removePersonCategory(Long categoryId, Long personId) {
        var personCategory = personCategoryRepository.findPersonCategory(personId);
        personCategory.ifPresent(pc -> {
            pc.setCategories(pc.getCategories().stream()
                    .filter(c -> !c.getId().equals(categoryId))
                    .collect(Collectors.toSet()));
            personCategoryRepository.save(pc);
        });
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public PersonCategory addPersonCategories(@Nonnull List<Long> categoryIds, @Nonnull Long personId) {
        checkNotNull(personId, ERROR_CHECK_NOT_NULL, "personId");
        checkNotNull(categoryIds, ERROR_CHECK_NOT_NULL, "categories");

        List<Category> categories = getCategories(categoryIds);

        checkIfAssignable(categories);

        PersonCategory pc = personCategoryRepository
                .findPersonCategory(personId)
                .orElseGet(PersonCategory::new);

        pc.setPersonId(personId);
        pc.setCategories(new HashSet<>(categories));

        return personCategoryRepository.save(pc);
    }

    @Transactional
    @Override
    public PersonCategory initPersonCategories(String authorizationHeader, List<Long> categoryIds, Long personId) {
        PersonCategory personCategory = addPersonCategories(categoryIds, personId);
        kafkaProducerService.sendMessage(CommonConstants.INTEREST_GROUP_ONBOARD_STATE, OnboardDTO
                .builder()
                .personId(personId)
                .state(OnboardState.INTERESTS_PROVIDED)
                .build());

        return personCategory;
    }

    @Transactional
    @Override
    public void removePersonCategories(Long personId) {
        personCategoryRepository.removePersonCategories(personId);
    }

    @Scheduled(cron = "${social.groupservice.statistics.cron}")
    @Transactional
    public void updateCategoriesStatistics() {
        LOGGER.debug("Updating categories with person and group counts... ");

        List<Category> categoriesToUpdate = new ArrayList<>();

        try {
            Map<Long, Long> idPersonCountMap = getIdPersonCountMap();
            Map<Long, Long> idGroupCountMap = getIdGroupCountMap();

            Set<Long> ids = new HashSet<>(idPersonCountMap.keySet());
            ids.addAll(idGroupCountMap.keySet());

            // Update categories with person and group counts:
            repository.findAllById(ids).forEach(c -> {
                long personCount = idPersonCountMap.get(c.getId()) != null ? idPersonCountMap.get(c.getId()) : 0;
                long groupCount = idGroupCountMap.get(c.getId()) != null ? idGroupCountMap.get(c.getId()) : 0;
                if (c.getPersonCount() != personCount || c.getGroupCount() != groupCount) {
                    c.setPersonCount(personCount);
                    c.setGroupCount(groupCount);
                    categoriesToUpdate.add(c);
                }
            });

            repository.saveAll(categoriesToUpdate);

        } catch (Exception ex) {
            LOGGER.error("Failed to update categories: {}", ex.getMessage());
        } finally {
            LOGGER.debug("Finished updating categories with person and group counts. Updated {} records.", categoriesToUpdate.size());
        }
    }

    @Override
    public CategoryStats getCategoryStats(String fromDate, TimeInterval timeInterval) {
        if (timeInterval == null) {
            timeInterval = TimeInterval.MONTH;
        }
        Timestamp timestamp = StringUtils.isEmpty(fromDate) ? null : Timestamp.valueOf(DateUtil.getDateFromString(fromDate).atStartOfDay());
        long allCategoriesCount = repository.getAllCategoriesCount();
        long newCategoriesCount = repository.getNewCategoriesCount(timestamp);

        CategoryStats stats = CategoryStats.builder()
                .allCategoriesCount(allCategoriesCount)
                .newCategoriesCount(newCategoriesCount)
                .fromDate(timestamp)
                .timeInterval(timeInterval)
                .build();

        switch (timeInterval) {
            case DAY -> {
                List<CreatedCategories> createdCategoriesPerDay = repository.getCreatedCategoriesPerDay(timestamp);
                stats.setCreatedCategories(createdCategoriesPerDay);
            }
            case YEAR -> {
                List<CreatedCategories> createdCategoriesPerYear = repository.getCreatedCategoriesPerYear(timestamp);
                stats.setCreatedCategories(createdCategoriesPerYear);
            }
            default -> {
                List<CreatedCategories> createdCategoriesPerMonth = repository.getCreatedCategoriesPerMonth(timestamp);
                stats.setCreatedCategories(createdCategoriesPerMonth);
            }
        }
        return stats;
    }

    public static void checkIfAssignable(List<Category> categories) {
        if (CollectionUtils.isEmpty(categories)) return;

        for (Category category : categories) {
            if (category.isRootCategory()) {
                throw new BadRequestException("error.root.category.not.assignable");
            }
        }
    }

    private Map<Long, Long> getIdGroupCountMap() {
        List<CategoryModel> categoryGroupCounts = repository.findGroupCountForCategories();

        Map<Long, Long> idCount = new HashMap<>();
        categoryGroupCounts.forEach(cm -> {
            Long id = cm.getId();
            idCount.merge(id, cm.getGroupCount(), Long::sum);
            Category parent = cm.getParentCategory();
            if (parent != null) {
                Long parentCategoryId = parent.getId();
                idCount.merge(parentCategoryId, cm.getGroupCount(), Long::sum);
            }
        });
        return idCount;
    }

    private Map<Long, Long> getIdPersonCountMap() {
        List<CategoryModel> categoryPersonCounts = repository.findPersonCountForCategories();

        Map<Long, Long> idCount = new HashMap<>();
        categoryPersonCounts.forEach(cm -> {
            Long id = cm.getId();
            idCount.merge(id, cm.getPersonCount(), Long::sum);
            Category parent = cm.getParentCategory();
            if (parent != null) {
                Long parentCategoryId = parent.getId();
                idCount.merge(parentCategoryId, cm.getPersonCount(), Long::sum);
            }
        });
        return idCount;
    }

    private void removeMediaFiles(Category category, Boolean isAdmin) {
        mediaService.removeAvatar(category, isAdmin);
        mediaService.removeCover(category, isAdmin);
    }


    private CategoryLocalized createCategoryLocalized(Category category, String categoryName, String locale) {
        return CategoryLocalized
                .builder()
                .locale(locale)
                .category(category)
                .name(categoryName)
                .build();
    }

    private void updateQuery(@Nonnull CategorySearchCriteria criteria) {
        String query = criteria.getQuery();
        if (Objects.nonNull(query)) {
            criteria.setQuery("%" + query.toLowerCase().trim().replaceAll("\\s+", "%") + "%");
        } else {
            criteria.setQuery("%");
        }
    }
}
