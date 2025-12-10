package uk.gov.moj.cpp.staging.prosecutors.spi.it;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.JMSTopicHelper.postMessageToTopicAndVerify;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.ASYNC_CJSE_RESPONSE_XML;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_FILE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.OIValidation_302_ERROR_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.OIValidation_ERRORS_ALL_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.OIValidation_ERRORS_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.OIValidation_INVALID_FIELD_VALUES;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.OIValidation_MALFORMED_XML;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.OIValidation_MANDATORY_FIELDS;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyErrorEvent;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyPrevioslySentError;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifySPImessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.CJSE_SYSTEM_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.CPP_SYSTEM_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.TEST_CONFIG_INSTANCE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.getLastPostedCommand;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.readFile;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.stubPCFcommand;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.ProsecutionCaseFileStub.stubGetProsecutionCaseFile;

import uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper;
import uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.SystemIdMapperStub;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class OIErrorsIT extends AbstractIT {

    public static final String MALFORMED_XML_ASYNC_RESPONSE_JSON = "external/message/errors/MalformedXmlAsyncResponse.json";
    private static final String CJSE_DELIVERY_URL = "/simulator/CJSE/message";

    @BeforeEach
    public void setUp() {
        TEST_CONFIG_INSTANCE.setUp();
    }

    @Test
    public void shouldSendSPIInWithoutMandatoryOnFieldsOIValidationErrors() throws Exception {
        sendAndVerifyErrorEvent(TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getRequestId(), OIValidation_MANDATORY_FIELDS, OIValidation_ERRORS_JSON);
    }

    @Test
    public void shouldSendSPIInWithWrongValuesOnOIFieldsOIValidationErrors() throws Exception {
        sendAndVerifyErrorEvent(TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getRequestId(), OIValidation_INVALID_FIELD_VALUES, OIValidation_ERRORS_ALL_JSON);
    }

    @Test
    public void shouldSendSPIInMessageWithSameCorrelationAndSystemIdAndExpectOIPreviouslySentValidationError() throws Exception {
        String requestId2ForPreviouslySent = randomUUID().toString();
        SystemIdMapperStub.stubGetRequestUUIDByMdiRequestId(requestId2ForPreviouslySent, randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_FILE, EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        sendAndVerifyPrevioslySentError(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_FILE, OIValidation_302_ERROR_JSON, requestId2ForPreviouslySent, TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void shouldSendAsyncErrorRespWhenMalformedPayload() throws Exception {
        SPIInSoapHelper spiInSoapHelper = new SPIInSoapHelper(TEST_CONFIG_INSTANCE.getURN());

        final String spiInMessageWithURN = spiInSoapHelper.replaceURNInSPI(readFile(OIValidation_MALFORMED_XML)).replace("CORRELATION_ID", TEST_CONFIG_INSTANCE.getCorrelationId());
        spiInSoapHelper.sendSPIInMessage(spiInMessageWithURN, TEST_CONFIG_INSTANCE.getRequestId());
        spiInSoapHelper.validateCJSEEvent();
        spiInSoapHelper.validateCppMessagePreparedEvent(MALFORMED_XML_ASYNC_RESPONSE_JSON, TEST_CONFIG_INSTANCE.getCorrelationId());
    }


    @Test
    public void shouldRecordAsyncErrorRespForCPPResponse() throws Exception {
        SPIInSoapHelper spiInSoapHelper = new SPIInSoapHelper(TEST_CONFIG_INSTANCE.getURN());

        final String spiInMessageWithURN = spiInSoapHelper.replaceURNInSPI(readFile(OIValidation_MALFORMED_XML)).replace("CORRELATION_ID", TEST_CONFIG_INSTANCE.getCorrelationId());
        spiInSoapHelper.sendSPIInMessage(spiInMessageWithURN, TEST_CONFIG_INSTANCE.getRequestId());
        spiInSoapHelper.validateCJSEEvent();
        spiInSoapHelper.validateCppMessagePreparedEvent(MALFORMED_XML_ASYNC_RESPONSE_JSON, TEST_CONFIG_INSTANCE.getCorrelationId());
        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getCorrelationId()))).size(),
                        CoreMatchers.is(1));
        final String responseSent = findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL)).withRequestBody(containing(TEST_CONFIG_INSTANCE.getCorrelationId()))).stream().findFirst().get().getBodyAsString();
        final String responseCorrelationId = (responseSent.split("lt;CorrelationID&gt;")[1].substring(0, 36));
        final String spiMessage = readFile(ASYNC_CJSE_RESPONSE_XML).replace("CORRELATION_ID", responseCorrelationId).replace("SYSTEM_ID", CJSE_SYSTEM_ID);
        final String newRequestId = randomUUID().toString();
        SystemIdMapperStub.stubGetRequestUUIDByMdiRequestId(newRequestId, randomUUID());
        spiInSoapHelper.sendSPIInMessage(spiMessage, newRequestId);
        spiInSoapHelper.validateErrorResportedForCPPResponse();
    }

    @Test
    public void shouldRecordAsyncErrorRespFromCjse() throws Exception {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        SPIInSoapHelper spiInSoapHelper = new SPIInSoapHelper(TEST_CONFIG_INSTANCE.getURN());

        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        UUID oid = randomUUID();
        SystemIdMapperStub.stubGetRequestUUIDBySystemId((CPP_SYSTEM_ID), oid);
        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        CoreMatchers.is(1));

        final String responseSent = getLastPostedCommand();
        final String responseCorrelationId = (responseSent.split("lt;CorrelationID&gt;")[1].substring(0, 36));
        final String spiMessage = readFile(ASYNC_CJSE_RESPONSE_XML).replace("CORRELATION_ID", responseCorrelationId).replace("SYSTEM_ID", CPP_SYSTEM_ID);
        final String newRequestId = randomUUID().toString();
        SystemIdMapperStub.stubGetRequestUUIDByMdiRequestId(newRequestId, randomUUID());
        SystemIdMapperStub.stubGetRequestUUIDBySystemId((CPP_SYSTEM_ID), oid);
        spiInSoapHelper.sendSPIInMessage(spiMessage, newRequestId);
        spiInSoapHelper.validateErrorResportedForCPPResponse();
    }
}
