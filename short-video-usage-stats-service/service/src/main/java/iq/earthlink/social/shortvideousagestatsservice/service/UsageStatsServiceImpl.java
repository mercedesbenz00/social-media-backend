package iq.earthlink.social.shortvideousagestatsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.shortvideousagestatsservice.ShortVideoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class UsageStatsServiceImpl implements UsageStatsService {

    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    public UsageStatsServiceImpl(ObjectMapper objectMapper, KafkaProducerService kafkaProducerService) {
        this.objectMapper = objectMapper;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    public void addEvent(String data) {
        try {
            ShortVideoEvent event = objectMapper.readValue(data, ShortVideoEvent.class);
            kafkaProducerService.sendObjectMessage("usage_stats", event);
        } catch (Exception e) {
            log.error("Exception in usage stats add event: {}", e.getMessage());
            kafkaProducerService.sendStringMessage("usage_stats_dead_topic", data);
            throw new BadRequestException("error.usage.stat.wrong.data");
        }
    }
}
