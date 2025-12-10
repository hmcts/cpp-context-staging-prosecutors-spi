package uk.gov.moj.cpp.staging.soap.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.soap.schema.BigDecimalFormatter.parseBigDecimal;
import static uk.gov.moj.cpp.staging.soap.schema.BigDecimalFormatter.printBigDecimal;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class BigDecimalFormatterTest {

    private static final String EXPECTED_POSITIVE_VALUE = "+10.00";
    private static final String EXPECTED_NEGATIVE_VALUE = "-10.00";

    @Test
    public void shouldHavePlusSign() {
        assertThat(printBigDecimal(parseBigDecimal("10")), is(EXPECTED_POSITIVE_VALUE));
    }

    @Test
    public void shouldHaveMinusSign() {
        assertThat(printBigDecimal(parseBigDecimal("-10")), is(EXPECTED_NEGATIVE_VALUE));
    }

    @Test
    public void shouldHaveNoSign() {
        assertThat(printBigDecimal(parseBigDecimal("0")), is("0.00"));
    }
}