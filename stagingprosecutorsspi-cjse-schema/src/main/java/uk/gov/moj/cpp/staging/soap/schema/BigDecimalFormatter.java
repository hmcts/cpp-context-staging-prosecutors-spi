package uk.gov.moj.cpp.staging.soap.schema;

import static java.lang.String.format;

import java.math.BigDecimal;

public class BigDecimalFormatter {

    private static final int DECIMAL_PLACES = 2;

    private BigDecimalFormatter() {
    }

    public static String printBigDecimal(final BigDecimal value) {
        final String valueAsString = value.toString();
        return value.signum() > 0 ? format("+%s", valueAsString) : valueAsString;
    }

    public static BigDecimal parseBigDecimal(final String value) {
        return new BigDecimal(value).setScale(DECIMAL_PLACES, BigDecimal.ROUND_DOWN);
    }

}