package uk.gov.moj.cpp.staging.prosecutors.spi.it;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.JMSTopicHelper.postMessageToTopicAndVerify;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifySPImessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.TEST_CONFIG_INSTANCE;

import uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils;

import java.io.IOException;

import javax.xml.datatype.DatatypeConfigurationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SPIEjectCaseIT extends AbstractIT {

    private static final String CASE_OR_APPLICATION_EJECTED_PUBLIC_EVENT = "public.progression.events.case-or-application-ejected";
    public static final String EXPECTED_EVENT_TO_BE_PUBLISHED = "stagingprosecutorsspi.event.spi-police-case-ejected";

    private static String PROSECUTOR_CASE_ID;


    @BeforeEach
    public void setUp() throws IOException, DatatypeConfigurationException {
        TEST_CONFIG_INSTANCE.setUp();
        PROSECUTOR_CASE_ID = TEST_CONFIG_INSTANCE.getCaseId().toString();
        TestUtils.stubPCFcommand(randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void testCaseEjected_whenCaseEjectedPublicEventIsRaised_expectSPIPoliceCaseEjectedPrivateEvent() {

        final String payloadForEjectedCasePublicEvent = createObjectBuilder()
                .add("hearingIds", createArrayBuilder().add(randomUUID().toString()).build())
                .add("prosecutionCaseId", PROSECUTOR_CASE_ID)
                .add("removalReason", "legal")
                .build().toString();
        postMessageToTopicAndVerify(payloadForEjectedCasePublicEvent, CASE_OR_APPLICATION_EJECTED_PUBLIC_EVENT, EXPECTED_EVENT_TO_BE_PUBLISHED);

    }

}
