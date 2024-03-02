package iq.earthlink.social.groupservice.category;

import iq.earthlink.social.groupservice.category.repository.CategoryRepository;
import iq.earthlink.social.groupservice.category.repository.PersonCategoryRepository;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import iq.earthlink.social.groupservice.group.DefaultGroupManager;
import iq.earthlink.social.groupservice.group.GroupManagerUtils;
import iq.earthlink.social.groupservice.group.GroupRepository;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;


class CategoryStatisticsTests {

    private List<Long> categoryIds;
    private List<Category> categories;
    private PersonInfo personInfo;

    @InjectMocks
    private DefaultCategoryManager categoryManager;

    @InjectMocks
    private DefaultGroupManager groupManager;

    @Mock
    private GroupManagerUtils groupManagerUtils;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    PersonCategoryRepository personCategoryRepository;

    @Mock
    GroupRepository groupRepository;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);

        categoryIds = List.of(2L, 3L, 4L, 5L, 6L, 7L);
        categories = getCategories(categoryIds, 0, 0);
        personInfo = JsonPersonProfile.builder().id(1L).roles(Set.of("ADMIN")).build();

        given(categoryRepository.findAllById(categoryIds)).willReturn(categories);
        given(personCategoryRepository.findPersonCategory(personInfo.getId())).willReturn(Optional.empty());
    }

    @Test
    void whenCategoryAssignedToPerson_updateCategoryPersonCount() {
        List<Category> categoriesUpdated = getCategories(categoryIds, 1, 0);

        categoryManager.addPersonCategories(categoryIds, personInfo.getId());
        categoryManager.updateCategoriesStatistics();

        given(categoryRepository.findById(2L)).willReturn(Optional.ofNullable(categoriesUpdated.get(0)));

        Category updatedCat = categoryManager.getCategory(2L);
        assertThat(updatedCat.getPersonCount()).isEqualTo(1);
    }

    private List<Category> getCategories(List<Long> categoryIds, long personCount, long groupCount) {
        final String TEST_CATEGORY = "Test Category ";
        final String TEST_CATEGORY_1 = "Test Category 1";
        List<Category> categoryList = new ArrayList<>();
        Map<String, CategoryLocalized> localizations = new HashMap<>();
        categoryIds.forEach(id -> {
            localizations.put("en", new CategoryLocalized.CategoryLocalizedBuilder()
                    .category(new Category.CategoryBuilder().id(id).name(TEST_CATEGORY + id).build())
                    .locale("en")
                    .name("Test Category Localized en" + id)
                    .build());
            localizations.put("ar", new CategoryLocalized.CategoryLocalizedBuilder()
                    .category(new Category.CategoryBuilder().id(id).name(TEST_CATEGORY + id).build())
                    .locale("ar")
                    .name("Test Category Localized ar" + id)
                    .build());
        });
        localizations.put("en", new CategoryLocalized.CategoryLocalizedBuilder()
                .category(new Category.CategoryBuilder().id(1L).name(TEST_CATEGORY_1).build())
                .locale("en")
                .name("Test Category Localized Parent en")
                .build());
        localizations.put("ar", new CategoryLocalized.CategoryLocalizedBuilder()
                .category(new Category.CategoryBuilder().id(1L).name(TEST_CATEGORY_1).build())
                .locale("ar")
                .name("Test Category Localized Parent ar")
                .build());
        Category parent = new Category.CategoryBuilder()
                .id(1L).localizations(localizations)
                .name(TEST_CATEGORY_1)
                .parentCategory(null)
                .categoryUUID(UUID.randomUUID())
                .avatar(null)
                .cover(null)
                .deleted(false)
                .createdAt(new Date(System.currentTimeMillis()))
                .groupCount(groupCount)
                .personCount(personCount)
                .build();
        categoryIds.forEach(id -> categoryList.add(new Category.CategoryBuilder()
                .id(id).localizations(localizations)
                .name(TEST_CATEGORY + id)
                .parentCategory(parent)
                .categoryUUID(UUID.randomUUID())
                .avatar(null)
                .cover(null)
                .deleted(false)
                .createdAt(new Date(System.currentTimeMillis()))
                .groupCount(groupCount)
                .personCount(personCount)
                .build()));
        return categoryList;
    }
}