package uk.gov.moj.cpp.staging.soap.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.soap.schema.OffenceSequenceNumberFormatter.parseInt;
import static uk.gov.moj.cpp.staging.soap.schema.OffenceSequenceNumberFormatter.printInt;

import org.junit.jupiter.api.Test;

public class OffenceSequenceNumberFormatterTest {

    private static final String EXPECTED_VALUE = "010";

    @Test
    public void shouldHaveLeadingZeros() {
        assertThat(printInt(parseInt("10")), is(EXPECTED_VALUE));
    }
}