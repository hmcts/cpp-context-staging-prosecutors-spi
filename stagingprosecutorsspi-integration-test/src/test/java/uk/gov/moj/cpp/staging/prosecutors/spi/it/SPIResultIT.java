package uk.gov.moj.cpp.staging.prosecutors.spi.it;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.JMSTopicHelper.postMessageToTopicAndVerify;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.QueryHelper.verifySpiMessageNotFound;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_WITHOUT_DATE_OF_BIRTH_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITHOUT_DATE_OF_BIRTH;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITH_NULL_ORG_NULL_DATA_CONTROLLER;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifySPImessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.CPP_SYSTEM_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.TEST_CONFIG_INSTANCE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.extractOIMessageFromPayload;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.extractValueFromXmlString;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.getLastPostedCommand;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.readFile;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.stubPCFcommand;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.verifyMdiMessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.verifyOIMessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.verifyResultedCaseMessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.verifyResultedCaseMessageUsingQuery;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.ProsecutionCaseFileStub.stubGetProsecutionCaseFile;

import uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.ReferenceDataStub;
import uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.SystemIdMapperStub;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

public class SPIResultIT extends AbstractIT {


    private static final String CJSE_DELIVERY_URL = "/simulator/CJSE/message";
    private static final Logger LOGGER = LoggerFactory.getLogger(SPIResultIT.class);
    public static final String NON_POLICE_SYSTEM_ID = "00001NPPforB7";

    private static Document convertStringToXMLDocument(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlString)));
    }

    @BeforeAll
    public static void setUpOnce() {
        ReferenceDataStub.stubPleaTypeGuiltyFlags();
        SystemIdMapperStub.stubGetRequestUUIDBySystemId((CPP_SYSTEM_ID), randomUUID());
    }

    @BeforeEach
    public void setUp() {
        TEST_CONFIG_INSTANCE.setUp();
        resetAllRequests();
    }

    @Test
    public void shouldGenerateSpiOut() throws IOException, DatatypeConfigurationException, SAXException {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        final String caseUrn = TEST_CONFIG_INSTANCE.getURN();
        sendAndVerifySPImessage(caseUrn, INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", caseUrn);
        final String defendantProsecutorReference = randomAlphanumeric(15);
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("TFL", defendantProsecutorReference);

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(caseUrn))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(caseUrn);
        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayload.xml");
        verifyOIMessage(lastPostedCommand, "mockFiles/expectedSpiOutOiPayload.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessage.xml");

        String expectedResultedCaseMessage = readFile("mockFiles/expectedResultedCaseMessage.xml");
        verifyResultedCaseMessageUsingQuery(caseUrn, defendantProsecutorReference, "Defendant's details changed", expectedResultedCaseMessage);

        // Send amended payload for SPI OUT and validate new message is returned by query
        final String amendedResultLabelAndValue = randomAlphabetic(30);
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replaceAll("Conditional discharge", amendedResultLabelAndValue);
        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        expectedResultedCaseMessage = expectedResultedCaseMessage.replaceAll("Conditional discharge", amendedResultLabelAndValue);
        verifyResultedCaseMessageUsingQuery(caseUrn, defendantProsecutorReference, amendedResultLabelAndValue, expectedResultedCaseMessage);
    }

    @Test
    public void shouldGenerateSpiOutWithNullOrganisationUnitAndNullDataController() throws IOException, DatatypeConfigurationException, SAXException {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITH_NULL_ORG_NULL_DATA_CONTROLLER, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        final String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated.json").getFile()))
                .replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayloadWithNullOrgAndNullDataController.xml");
        verifyOIMessage(lastPostedCommand, "mockFiles/expectedSpiOutOiPayloadWithNullOrgNullAndDataController.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessage.xml");
    }

    @Test
    public void shouldGenerateSpiOutWithEmptyBailStatus() throws IOException, DatatypeConfigurationException, SAXException {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-with_empty_bailstatus.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayload.xml");
        verifyOIMessage(lastPostedCommand, "mockFiles/expectedSpiOutOiPayload.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithEmptyBailStatus.xml");
    }

    @Test
    public void shouldGenerateSendSpiResultCommandWithNonPoliceSystemIdWhenPoliceSystemIdNotPresent() throws IOException {
        final String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated.json").getFile()))
                .replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        assertThat(extractValueFromXmlString(lastPostedCommand, "RouteDestinationSystem"), containsString(NON_POLICE_SYSTEM_ID));
        assertThat(extractValueFromXmlString(lastPostedCommand, "OrganizationalUnitID"), nullValue());
        assertThat(extractValueFromXmlString(lastPostedCommand, "DataController"), nullValue());
    }

    @Test
    public void shouldGenerateSpiOutWithEmailForIndividualPrimaryEmail() throws Exception {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITH_NULL_ORG_NULL_DATA_CONTROLLER, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-primary-email-contact.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayloadWithNullOrgAndNullDataController.xml");
        verifyOIMessageWithDataStreamContent(lastPostedCommand, "mockFiles/expectedSpiOutOiPayloadWithEmail.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithEmailForIndividualPrimaryEmail.xml");

    }


    @Test
    public void shouldGenerateSpiOutWithEmailForIndividualSecondaryEmail() throws Exception {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITH_NULL_ORG_NULL_DATA_CONTROLLER, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-secondary-email-contact.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayloadWithNullOrgAndNullDataController.xml");
        verifyOIMessageWithDataStreamContent(lastPostedCommand, "mockFiles/expectedSpiOutOiPayloadWithSecondaryEmail.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithEmailForIndividualSecondaryEmail.xml");

    }


    @Test
    public void shouldGenerateSpiOutWithEmailForIndividualPrimarySecondaryEmail() throws Exception {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITH_NULL_ORG_NULL_DATA_CONTROLLER, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-primary-both-email-contact.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayloadWithNullOrgAndNullDataController.xml");
        verifyOIMessageWithDataStreamContent(lastPostedCommand, "mockFiles/expectedSpiOutOiPayloadWithBothEmail.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithEmailForIndividualPrimarySecondaryEmail.xml");

    }


    @Test
    public void shouldGenerateSpiOutWithNoEmailForIndividual() throws Exception {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITH_NULL_ORG_NULL_DATA_CONTROLLER, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-no-email-contact.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayloadWithNullOrgAndNullDataController.xml");
        verifyOIMessageWithDataStreamContent(lastPostedCommand, "mockFiles/expectedSpiOutOiPayloadWithNoEmail.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithNoEmailForIndividual.xml");

    }

    @Test
    public void shouldGenerateSpiOutForIndividualWithoutDateOfBirth() throws Exception {
        stubPCFcommand(randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITHOUT_DATE_OF_BIRTH, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_WITHOUT_DATE_OF_BIRTH_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-without-date-of-birth.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayload.xml");
        verifyOIMessage(lastPostedCommand, "mockFiles/expectedSpiOutOiPayload.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageForIndividualWithoutDateOfBirth.xml");

    }

    @Test
    public void shouldGenerateSpiOutWithMultipleOffenceAndOneOfTheOffenceHaveOffenceLocation() throws Exception {
        stubPCFcommand(randomUUID());
        final String caseFileDraft = readFile("mockFiles/prosecutioncasefile-exact-match.json");
        final String caseFile = caseFileDraft
                .replaceAll("%CASE_ID%", "18ca70bd-4084-417f-9925-500b0223d99f")
                .replaceAll("%DEFENDANT_ID%", "0108fd5f-a9df-495b-93a9-543c64ca7d2c")
                .replaceAll("%OFFENCE_ID%", "00467bb1-a8db-4d07-866a-b1d5c8ad001e");
        stubGetProsecutionCaseFile(caseFile);
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITH_NULL_ORG_NULL_DATA_CONTROLLER, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/results.police-result-generated-for-offence-loc.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayloadWithNullOrgAndNullDataController.xml");
        verifyOIMessageWithDataStreamContent(lastPostedCommand, "mockFiles/expectedSpiOutOiPayloadWithOffenceLocation.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithMultipleOffenceAndOneOfTheOffenceHaveOffenceLocation.xml");

    }

    @Test
    public void shouldGenerateSpiOutWithPleaAndVehicleCode() throws Exception {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-with-plea.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayload.xml");
        verifyOIMessage(lastPostedCommand, "mockFiles/expectedSpiOutOiPayloadWithPlea.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithPlea.xml");

    }

    @Test
    public void shouldGenerateSpiOutWithIndicatedPlea() throws Exception {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-with-indicated-plea.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN())
                .replace("INDICATED_PLEA_VALUE", "INDICATED_GUILTY");

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayload.xml");
        verifyOIMessage(lastPostedCommand, "mockFiles/expectedSpiOutOiPayloadWithPlea.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithIndicatedPlea.xml");

    }

    @Test
    public void shouldGenerateSpiOutWithIndicatedNotGuiltyPlea() throws Exception {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-with-indicated-plea.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN())
                .replace("INDICATED_PLEA_VALUE", "INDICATED_NOT_GUILTY");

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());

        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayload.xml");
        verifyOIMessage(lastPostedCommand, "mockFiles/expectedSpiOutOiPayloadWithPlea.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithIndicatedNotGuiltyPlea.xml");

    }

    private void verifyOIMessageWithDataStreamContent(final String lastPostedCommand, String expectedFile) throws Exception {
        final Diff diff;
        String message = StringEscapeUtils.unescapeXml(extractOIMessageFromPayload(lastPostedCommand));
        String expectedSpiOutOiPayload = readFile(expectedFile);
        message = message.replaceAll("&lt;", "<");
        message = message.replaceAll("&gt;", ">");
        expectedSpiOutOiPayload = expectedSpiOutOiPayload.replaceAll("&lt;", "<");
        expectedSpiOutOiPayload = expectedSpiOutOiPayload.replaceAll("&gt;", ">");
        final Document expectedDoc = convertStringToXMLDocument(expectedSpiOutOiPayload);
        LOGGER.info("verifyOIMessageWithDataStreamContent - actual message" + System.lineSeparator() + message + System.lineSeparator());
        LOGGER.info("\n verifyOIMessageWithDataStreamContent - expected message" + System.lineSeparator() + expectedSpiOutOiPayload + System.lineSeparator());
        final Document actualMessage = convertStringToXMLDocument(message);
        assertThat(actualMessage.getElementsByTagName("CorrelationID").item(0).getFirstChild().getNodeValue(), is(actualMessage.getElementsByTagName("ns2:SystemDataStreamID").item(0).getFirstChild().getNodeValue()));
        diff = DiffBuilder
                .compare(expectedDoc)
                .withTest(actualMessage)
                .withNodeFilter(node -> !node.getNodeName().equals("CorrelationID")
                        && !node.getNodeName().equals("ns2:SystemDataStreamID")
                        && !node.getNodeName().equals("PTIURN")
                        && !node.getNodeName().equals("OffenceWording")
                        && !node.getNodeName().equals("ResultText")
                )
                .build();

        assertTrue(!diff.hasDifferences(), diff.toString());
    }

    @Test
    public void shouldGenerateSpiOutWithResultsFromSecondaryCjsCodesWhenAvailable() throws IOException, DatatypeConfigurationException, SAXException {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-with-secondary-cjs-codes.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand();
        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayload.xml");
        verifyOIMessage(lastPostedCommand, "mockFiles/expectedSpiOutOiPayload.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithSecondaryCjsCodes.xml");
    }

    @Test
    public void shouldGenerateSpiOutWithoutCjsResultCode() throws IOException, DatatypeConfigurationException, SAXException {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        String publicResultsPoliceResultGeneratedPayload = readFileToString(new File(this.getClass().getClassLoader().getResource("external/message/public.results.police-result-generated-without-cjscode.json").getFile()));
        publicResultsPoliceResultGeneratedPayload = publicResultsPoliceResultGeneratedPayload.replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        postMessageToTopicAndVerify(publicResultsPoliceResultGeneratedPayload, "public.results.police-result-generated", "stagingprosecutorsspi.event.spi-result-prepared-for-sending");

        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(() -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size(),
                        is(1));
        final String lastPostedCommand = getLastPostedCommand(TEST_CONFIG_INSTANCE.getURN());
        verifyMdiMessage(lastPostedCommand, "mockFiles/expectedSPIOutPayload.xml");
        verifyOIMessage(lastPostedCommand, "mockFiles/expectedSpiOutOiPayload.xml");
        verifyResultedCaseMessage(lastPostedCommand, "mockFiles/expectedResultedCaseMessageWithoutCjscode.xml");
    }

    @Test
    public void shouldGenerateNotFoundResponseWhenQueryingSpiResultsForNonExistentCase() {
        verifySpiMessageNotFound(randomAlphanumeric(8), randomAlphanumeric(15));

    }

}

