package uk.gov.moj.cpp.staging.soap.schema;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;

import java.io.File;

import org.junit.jupiter.api.Test;

public class ObjectUnMarshallerTest {

    @Test
    public void PoliceCaseStructureSchemaValidationTest() throws Exception {

        final String payload = readFileToString(new File(this.getClass().getClassLoader().getResource("spiInCaseExamples/IndividualDefendantAllfields.xml").getFile()));

        final ObjectUnMarshaller objectUnMarshaller = new ObjectUnMarshaller();
        final RouteDataRequestType routeDataRequestType = objectUnMarshaller.getRouteDataRequestType(payload);
        assertNotNull(routeDataRequestType);
        assertEquals("1.0", routeDataRequestType.getVersionNumber());
    }
}