package uk.gov.moj.cpp.staging.prosecutors.spi.utils;


import java.time.ZonedDateTime;

public class ZonedDateTimeUtil {

    /*
    * FIXME: Tactical solution to avoid having dates where nano seconds have a right most
    * zero as currently the framework is removing them when generating an envelope from
    * an event object.
    * */
    public static ZonedDateTime removeRightMostZeroes(ZonedDateTime zonedDateTime) {
        if (zonedDateTime.getNano() % 10000000 == 0) {
            return zonedDateTime.plusNanos(1000000);
        }
        return zonedDateTime;
    }
}
