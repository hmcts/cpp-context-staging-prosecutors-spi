package uk.gov.moj.cpp.casefilter.azure.utils;

import static java.lang.Boolean.TRUE;
import static java.time.LocalDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class DateTimeProviderTest {

    private static final String REGEX = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";
    //This test is going to have flaky behaviour as DateTimeProvider class is returning time by omitting seconds if seconds are 00, hence above regex need to be adjusted if that is accepted behaviour, otherwise test will fail if it gets executed exactly on 00 seconds
    private final  DateTimeProvider dateTimeProvider = new DateTimeProvider();

    @Test
    public void shouldSuccessfullyProvideDateTime() {
        final String utcZonedDateTimeString = dateTimeProvider.getUTCZonedDateTimeString();

        assertThat(utcZonedDateTimeString, utcZonedDateTimeString.matches(REGEX), is (TRUE));
    }

    @Test
    public void shouldSuccessfullyProvideDateTimeWithInput() {
        final String utcZonedDateTimeString = dateTimeProvider.getUTCZonedDateTimeString(now());

        assertThat(utcZonedDateTimeString, utcZonedDateTimeString.matches(REGEX), is (TRUE));
    }
}