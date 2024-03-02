package iq.earthlink.social.userfeedaggregatorservice.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class Utils {

    public boolean isTimestampExpired(Long timestamp, Integer thresholdMinutes) {
        Long currentTimestamp = getTimestamp();
        long timeDifferenceMillis = Math.abs(currentTimestamp - timestamp);
        long timeDifferenceMinutes = TimeUnit.MILLISECONDS.toMinutes(timeDifferenceMillis);

        return timeDifferenceMinutes > thresholdMinutes;

    }

    public Long getTimestamp() {
        LocalDateTime localDateTime = LocalDateTime.now();
        ZoneId zoneId = ZoneId.systemDefault();
        Instant instant = localDateTime.atZone(zoneId).toInstant();

        return instant.toEpochMilli();
    }

    public String calculateHash(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return null;
        }
    }
}
