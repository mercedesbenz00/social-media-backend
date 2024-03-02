package iq.earthlink.social.groupservice.group;

import iq.earthlink.social.exception.RestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

class GroupManagerUtilsTest {

    @InjectMocks
    private GroupManagerUtils groupManagerUtils;
    @Mock
    private GroupRepository groupRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getGroup_getByInvalidGroupId_throwRestApiException() {
        //given
        Long groupId = Long.MAX_VALUE;

        //when
        //then
        assertThatThrownBy(() -> groupManagerUtils.getGroup(groupId))
                .isInstanceOf(RestApiException.class)
                .hasMessageContaining("error.group.not.found");
    }

    @Test
    void getGroup_getByValidGroupId_returnGroupInfo() {
        //given
        UserGroup group = UserGroup.builder()
                .id(1L)
                .name("test group")
                .build();

        given(groupRepository.findById(group.getId())).willReturn(Optional.of(group));

        //when
        UserGroup foundGroupById = groupManagerUtils.getGroup(group.getId());

        //then
        assertEquals(foundGroupById.getId(), group.getId());
        assertEquals(foundGroupById.getName(), group.getName());
    }

}