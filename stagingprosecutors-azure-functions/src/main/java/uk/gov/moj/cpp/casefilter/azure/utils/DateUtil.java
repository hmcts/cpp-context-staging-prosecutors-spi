package uk.gov.moj.cpp.casefilter.azure.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DateUtil {

    private DateUtil(){ }

    public static Date getDate(final int hour, final int minute, final int second ){
        final LocalDateTime currentDateTime = LocalDateTime.now();
        final LocalDateTime startDateTime = currentDateTime.withHour(hour).withMinute(minute).withSecond(second);
        return Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
