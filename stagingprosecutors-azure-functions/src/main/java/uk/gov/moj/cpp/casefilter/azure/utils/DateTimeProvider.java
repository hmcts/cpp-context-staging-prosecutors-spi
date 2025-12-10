package uk.gov.moj.cpp.casefilter.azure.utils;

import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.of;

import java.time.LocalDateTime;

public class DateTimeProvider {

    public String getUTCZonedDateTimeString(final LocalDateTime localDateTime) {
        return of(localDateTime.withNano(0), UTC).toString();
    }

    public String getUTCZonedDateTimeString() {
        return getUTCZonedDateTimeString(now());
    }
}
