package iq.earthlink.social.common.util;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class DateUtil {
    private DateUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Gets date before current date with provided time interval
     *
     * @param timeInterval - interval in days
     * @return - previous date, Date
     */
    public static Date getDateBefore(int timeInterval) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -timeInterval);
        return cal.getTime();
    }

    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    public static LocalDate getDateFromString(String date) {
        return Objects.isNull(date) ? LocalDate.now() : LocalDate.parse(date);
    }

    public static long getDifferenceDays(Date d1, Date d2) {
        long diff = Math.abs(d2.getTime() - d1.getTime());
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }
}
