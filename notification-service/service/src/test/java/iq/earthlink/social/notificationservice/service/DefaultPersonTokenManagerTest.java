package iq.earthlink.social.notificationservice.service;

import iq.earthlink.social.notificationservice.data.model.PersonToken;
import iq.earthlink.social.notificationservice.data.repository.PersonTokenRepository;
import iq.earthlink.social.notificationservice.event.KafkaProducerService;
import iq.earthlink.social.notificationservice.service.token.DefaultPersonTokenManager;
import iq.earthlink.social.notificationservice.service.token.PersonTokenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultPersonTokenManagerTest {
    @InjectMocks
    private DefaultPersonTokenManager defaultPersonTokenManager;
    @Mock
    private PersonTokenRepository personTokenRepository;
    @Mock
    private PersonTokenProperties properties;
    @Mock
    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void addPushToken_addPushTokenToExistingPersonToken_returnPersonToken() {
        //given
        Long personId = 1L;
        String newPushToken = "new push token";
        String device = "web";
        PersonToken personToken = PersonToken.builder()
                .id(1L)
                .pushToken("push token")
                .personId(personId)
                .device(device)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        given(personTokenRepository.findByPersonIdAndDevice(personId, device)).willReturn(Optional.ofNullable(personToken));
        given(personTokenRepository.save(any())).will(returnsFirstArg());

        //when
        PersonToken actualPersonToken = defaultPersonTokenManager.addPushToken(personId, newPushToken, device);

        //then
        assertEquals(actualPersonToken.getPersonId(), personId);
        assertEquals(actualPersonToken.getDevice(), device);
        assertEquals(actualPersonToken.getPushToken(), newPushToken);
        verify(kafkaProducerService, times(1)).sendMessage(any(), any());
    }

    @Test
    void addPushToken_addPushTokenToNonExistingPersonToken_returnPersonToken() {
        //given
        Long personId = 1L;
        String newPushToken = "new push token";
        String device = "web";

        given(personTokenRepository.findByPersonIdAndDevice(personId, device)).willReturn(Optional.empty());
        given(personTokenRepository.save(any())).will(returnsFirstArg());

        //when
        PersonToken actualPersonToken = defaultPersonTokenManager.addPushToken(personId, newPushToken, device);

        //then
        assertEquals(actualPersonToken.getPersonId(), personId);
        assertEquals(actualPersonToken.getDevice(), device);
        assertEquals(actualPersonToken.getPushToken(), newPushToken);
        verify(kafkaProducerService, times(1)).sendMessage(any(), any());
    }

    @Test
    void deletePushToken() {
        //given
        Long personId = 1L;
        String pushToken = "push token";
        String device = "web";
        PersonToken personToken = PersonToken.builder()
                .id(1L)
                .pushToken("push token")
                .personId(personId)
                .device(device)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        given(personTokenRepository.findByPushTokenAndPersonId(pushToken, personId)).willReturn(Optional.ofNullable(personToken));

        //when
        defaultPersonTokenManager.deletePushToken(personId, pushToken);

        //then
        verify(personTokenRepository, times(1)).deleteByPushTokenAndPersonId(pushToken, personId);
        verify(kafkaProducerService, times(1)).sendMessage(any(), any());
    }
}