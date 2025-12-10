package uk.gov.moj.cpp.staging.prosecutors.spi.event.utils;

public class SpiUtil {

    private SpiUtil() {
    }

    public static <T, E> E ifNotNull(final T object, final E defaultValue) {
        return object == null ? null : defaultValue;
    }
}
