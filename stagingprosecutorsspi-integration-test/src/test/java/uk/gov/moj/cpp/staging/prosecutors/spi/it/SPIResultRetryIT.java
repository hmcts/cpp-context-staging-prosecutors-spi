package uk.gov.moj.cpp.staging.prosecutors.spi.it;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.JMSTopicHelper.postMessageToTopicAndVerify;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifySPImessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.CPP_SYSTEM_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.TEST_CONFIG_INSTANCE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.CJSE_DELIVERY_URL;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.getLastPostedCommand;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.readFile;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.stubPCFcommand;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.verifyMdiMessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.verifyOIMessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.CJSESoapServiceStubForRetry.stubCJSESoapService;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.ProsecutionCaseFileStub.stubGetProsecutionCaseFile;

import uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.SystemIdMapperStub;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class SPIResultRetryIT extends AbstractIT {


    @BeforeEach
    public void setUp() throws Exception {
        TEST_CONFIG_INSTANCE.setUp();
        resetAllRequests();
        SystemIdMapperStub.stubGetRequestUUIDBySystemId((CPP_SYSTEM_ID),randomUUID());
    }

    @Test
    public void shouldRetryAndGenerateSPIOutWhenInvalidResponseFromCJSE() throws IOException, DatatypeConfigurationException, SAXException {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        stubCJSESoapService();
        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.cppmessage-retry-delay-required");

        await().timeout(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS) //It can't be reduced further, as it should at least be >= cjse.retry.duration value
                .pollDelay(2, TimeUnit.SECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        CoreMatchers.is(2));
        final String lastPostedCommand = getLastPostedCommand();

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayload.xml");

        verifyOIMessage(lastPostedCommand, "mockFiles/expectedSpiOutOiPayload.xml");

    }


}


