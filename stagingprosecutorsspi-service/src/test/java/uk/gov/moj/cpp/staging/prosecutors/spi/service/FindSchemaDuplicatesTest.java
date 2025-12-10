package uk.gov.moj.cpp.staging.prosecutors.spi.service;

import org.junit.jupiter.api.Test;
import uk.gov.justice.services.test.utils.core.schema.SchemaDuplicateTestHelper;
@SuppressWarnings({"squid:S1607"})
class FindSchemaDuplicatesTest {

    @Test
    void shouldFindSchemaDuplicatesTest() {
       SchemaDuplicateTestHelper.failTestIfDifferentSchemasWithSameName();
    }
}
