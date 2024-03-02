package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.groupservice.category.Category;
import iq.earthlink.social.groupservice.category.CategoryMediaService;
import iq.earthlink.social.groupservice.category.DefaultCategoryManager;
import iq.earthlink.social.groupservice.category.PersonCategory;
import iq.earthlink.social.groupservice.category.dto.CategoryRequest;
import iq.earthlink.social.groupservice.category.repository.CategoryRepository;
import iq.earthlink.social.groupservice.category.repository.PersonCategoryRepository;
import iq.earthlink.social.groupservice.event.KafkaProducerService;
import iq.earthlink.social.groupservice.util.RoleUtil;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonPerson;
import iq.earthlink.social.personservice.person.rest.JsonPersonProfile;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.security.DefaultSecurityProvider;
import org.dozer.DozerBeanMapperBuilder;
import org.dozer.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


class DefaultCategoryManagerTest {
    private static final String AUTHORIZATION_HEADER = "authorizationHeader";
    private static final String MY_CATEGORY = "My Category";
    private static final String MY_NEW_CATEGORY = "My New Category";

    @InjectMocks
    private DefaultCategoryManager defaultCategoryManager;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private PersonCategoryRepository personCategoryRepository;
    @Mock
    private PersonRestService personRestService;
    @Mock
    private CategoryMediaService mediaService;
    @Mock
    private DefaultSecurityProvider securityProvider;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Mock
    private RoleUtil roleUtil;
    @Spy
    private Mapper mapper = DozerBeanMapperBuilder.create().build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void createCategory_whenCategoryRequestIsValid_success() {
        // given
        PersonInfo person = getPerson();
        CategoryRequest req = new CategoryRequest();
        req.setName(MY_CATEGORY);
        Category category = new Category();
        category.setId(1L);
        category.setName(MY_CATEGORY);

        // when
        when(categoryRepository.saveAndFlush(any(Category.class))).thenReturn(category);
        when(securityProvider.getRolesFromAuthorization(AUTHORIZATION_HEADER)).thenReturn(any());
        when(securityProvider.getPersonIdFromAuthorization(AUTHORIZATION_HEADER)).thenReturn(person.getId());
        given(roleUtil.isAdmin(any())).willReturn(true);

        // then
        Category categoryResult = defaultCategoryManager.createCategory(AUTHORIZATION_HEADER, req);
        assertEquals(category.getName(), categoryResult.getName());
    }

    @Test
    void createCategory_whenCategoryDuplicate_throwNotUniqueException() {
        // given
        PersonInfo person = getPerson();
        CategoryRequest req = new CategoryRequest();
        req.setName(MY_CATEGORY);
        Category category = new Category();
        category.setId(1L);
        category.setName(MY_CATEGORY);

        // when
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(new NotUniqueException("Category name is not unique"));
        when(securityProvider.getRolesFromAuthorization(AUTHORIZATION_HEADER)).thenReturn(any());
        when(securityProvider.getPersonIdFromAuthorization(AUTHORIZATION_HEADER)).thenReturn(person.getId());
        given(roleUtil.isAdmin(any())).willReturn(true);

        // then
        assertThrows(NotUniqueException.class, () -> defaultCategoryManager.createCategory(AUTHORIZATION_HEADER, req));
    }

    @Test
    void updateCategory_whenCategoryRequestIsValid_success() {
        // given
        PersonInfo person = getPerson();
        CategoryRequest req = new CategoryRequest();
        req.setName(MY_NEW_CATEGORY);
        Category category = new Category();
        category.setId(1L);
        category.setName(MY_CATEGORY);

        // when
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.saveAndFlush(new Category())).thenReturn(category);

        // then
        Category result = defaultCategoryManager.updateCategory(1L, req, person.isAdmin());
        assertEquals(category.getName(), result.getName());
    }

    @Test
    void updateCategory_whenCategoryNotExist_throwNotFoundException() {
        // given
        PersonInfo person = getPerson();
        boolean isAdmin = person.isAdmin();
        CategoryRequest req = new CategoryRequest();
        req.setName(MY_NEW_CATEGORY);
        Category category = new Category();
        category.setId(1L);
        category.setName(MY_CATEGORY);

        // when
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());
        when(categoryRepository.saveAndFlush(new Category())).thenReturn(category);

        // then
        assertThrows(NotFoundException.class, () -> defaultCategoryManager.updateCategory(1L, req, isAdmin));
    }

    @Test
    void removeCategory_whenCategoryRequestIsValid_success() {
        // given
        PersonInfo person = getPerson();
        Category category = new Category();
        category.setId(1L);

        PersonCategory personCategory = new PersonCategory();
        personCategory.setCategories(new HashSet<>(List.of(category)));
        Page<PersonCategory> page = new PageImpl<>(Collections.singletonList(personCategory), PageRequest.of(0, 3), 1);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(categoryRepository.getOne(1L)).thenReturn(category);
        when(personCategoryRepository.getPersonCategoriesByCategoriesIn(any(), any())).thenReturn(page);

        // when
        defaultCategoryManager.removeCategory(1L, person.isAdmin());

        // then
        verify(categoryRepository, times(1)).deleteById(any());
    }

    @Test
    void removeCategory_whenCategoryNotExist_throwNotFoundException() {
        // given
        PersonInfo person = getPerson();
        boolean isAdmin = person.isAdmin();
        CategoryRequest req = new CategoryRequest();
        req.setName(MY_NEW_CATEGORY);
        Category category = new Category();
        category.setId(1L);
        category.setName(MY_CATEGORY);

        // when
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());
        when(categoryRepository.saveAndFlush(new Category())).thenReturn(category);

        // then
        assertThrows(NotFoundException.class, () -> defaultCategoryManager.updateCategory(1L, req, isAdmin));
    }

    @Test
    void addPersonCategory_whenCategoryRequestIsValid_success() {
        // given
        PersonInfo person = getPerson();
        Category category = new Category();
        category.setId(1L);
        category.setParentCategory(new Category());
        PersonCategory personCategory = new PersonCategory();
        personCategory.setPersonId(person.getId());
        // when
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(personCategoryRepository.save(any(PersonCategory.class))).thenReturn(personCategory);
        // then
        PersonCategory personCategoryResult = defaultCategoryManager.addPersonCategory(category.getId(), person.getId());
        assertEquals(person.getId(), personCategoryResult.getPersonId());
    }

    @Test
    void addPersonCategory_whenCategoryNotExist_throwNotFoundException() {
        // given
        Long personId = 1L;

        // when
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        // then
        assertThrows(NotFoundException.class, () -> defaultCategoryManager.addPersonCategory(1L, personId));
    }

    @Test
    void removePersonCategory_whenCategoryRequestIsValid_success() {
        // given
        PersonInfo person = getPerson();
        Category category = new Category();
        category.setId(1L);
        PersonCategory personCategory = new PersonCategory();
        personCategory.setPersonId(person.getId());
        personCategory.setCategories(Set.of(category));

        when(personCategoryRepository.findPersonCategory(person.getId())).thenReturn(Optional.of(personCategory));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        // when
        defaultCategoryManager.removePersonCategory(category.getId(), person.getId());

        // then
        verify(personCategoryRepository, times(1)).save(any());
    }

    private PersonInfo getPerson() {
        return JsonPersonProfile.builder()
                .id(1L)
                .roles(new HashSet<>(List.of("ADMIN")))
                .build();
    }

    @Test
    void initPersonCategories_whenCategoryRequestIsValid_success() {
        // given
        JsonPersonProfile person = JsonPersonProfile.builder().id(1L).build();
        List<Long> categoryIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);
        Category category = new Category();
        category.setId(1L);
        category.setParentCategory(new Category());
        Category category2 = new Category();
        category2.setId(2L);
        category2.setParentCategory(new Category());
        Category category3 = new Category();
        category3.setId(3L);
        category3.setParentCategory(new Category());
        Category category4 = new Category();
        category4.setId(4L);
        category4.setParentCategory(new Category());
        Category category5 = new Category();
        category5.setId(5L);
        category5.setParentCategory(new Category());
        Category category6 = new Category();
        category6.setId(6L);
        category6.setParentCategory(new Category());
        PersonCategory personCategory = new PersonCategory();
        personCategory.setPersonId(person.getId());
        personCategory.setCategories(Set.of(category, category2, category3, category4, category5, category6));
        JsonPerson jsonPerson = mapper.map(person, JsonPerson.class);

        // when
        when(personRestService.getPersonProfile(any())).thenReturn(person);
        when(categoryRepository.findAllById(categoryIds)).thenReturn(Arrays.asList(category, category2, category3, category4, category5, category6));
        when(personCategoryRepository.save(personCategory)).thenReturn(personCategory);


        // then
        PersonCategory personCategoryResult = defaultCategoryManager.initPersonCategories("Bearer token", categoryIds, person.getId());
        assertEquals(6, personCategoryResult.getCategories().size());
    }

    @Test
    void removePersonCategories_success() {
        // given
        PersonInfo person = JsonPersonProfile.builder().id(1L).build();

        // when
        defaultCategoryManager.removePersonCategories(person.getId());

        // then
        verify(personCategoryRepository, times(1)).removePersonCategories(any());
    }

}
