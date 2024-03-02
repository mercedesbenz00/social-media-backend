package iq.earthlink.social.shortvideousagestatsservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.shortvideousagestatsservice.ShortVideoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

class UsageStatsServiceImplTest {

    @Mock
    ObjectMapper objectMapper;

    @Mock
    KafkaProducerService kafkaProducerService;

    @InjectMocks
    private UsageStatsServiceImpl usageStatsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void addEventWithCorrectData_willNotThroughException() throws JsonProcessingException {
        //given
        String correctData = "{\"videoId\": \"123\"," +
                "    \"actionType\": \"viewed\"," +
                "    \"actionValue\": \"true\"}";
        ShortVideoEvent event = ShortVideoEvent
                .newBuilder()
                .setVideoId("123")
                .setActionType("viewed")
                .setActionValue("true")
                .build();
        given(objectMapper.readValue(anyString(), eq(ShortVideoEvent.class))).willReturn(event);
        willDoNothing().given(kafkaProducerService).sendObjectMessage(any(String.class), any(Object.class));
        willDoNothing().given(kafkaProducerService).sendStringMessage(any(String.class), any(String.class));
        //when
        usageStatsService.addEvent(correctData);
        //then
        then(kafkaProducerService).should().sendObjectMessage(any(String.class), any(Object.class));
        then(kafkaProducerService).should(never()).sendStringMessage(any(String.class), any(String.class));
    }


    @Test
    void addEventWithIncorrectData_willThroughException() throws JsonProcessingException {
        //given
        String inCorrectData = "{\"videoId\": \"123\"," +
                "    \"actionType2\": \"viewed\"," +
                "    \"actionValue2\": \"true\"}";

        given(objectMapper.readValue(anyString(), eq(ShortVideoEvent.class))).willThrow(new JsonMappingException("Error with ObjectMapper") {
        });
        willDoNothing().given(kafkaProducerService).sendObjectMessage(any(String.class), any(Object.class));
        willDoNothing().given(kafkaProducerService).sendStringMessage(any(String.class), any(String.class));
        //when
        //then
        assertThatThrownBy(() -> usageStatsService.addEvent(inCorrectData))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.usage.stat.wrong.data");

        then(kafkaProducerService).should(never()).sendObjectMessage(any(String.class), any(Object.class));
        then(kafkaProducerService).should().sendStringMessage(any(String.class), any(String.class));

    }
}