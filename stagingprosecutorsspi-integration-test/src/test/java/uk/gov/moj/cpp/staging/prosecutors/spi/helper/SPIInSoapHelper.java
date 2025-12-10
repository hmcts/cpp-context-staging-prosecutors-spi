package uk.gov.moj.cpp.staging.prosecutors.spi.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.JMSTopicHelper.postMessageToPublicTopic;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.SPISoapAdapterHelper.getReadUrl;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.SPISoapAdapterHelper.sendSubmitRequest;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.SubmitRequestBuilder.CMS_SOAP_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.SubmitRequestBuilder.CPP_SOAP_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.readFile;

import uk.gov.cjse.schemas.endpoint.types.ExecMode;
import uk.gov.cjse.schemas.endpoint.types.RetrieveRequest;
import uk.gov.cjse.schemas.endpoint.types.RetrieveResponse;
import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.cjse.schemas.endpoint.types.SubmitResponse;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.messaging.DeadLetterQueueBrowser;
import uk.gov.moj.cpp.staging.prosecutors.spi.utils.SPISoapAdapterHelper;
import uk.gov.moj.cpp.staging.prosecutors.spi.utils.SubmitRequestBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.xml.datatype.DatatypeConfigurationException;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class SPIInSoapHelper {

    public static final String EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON = "external/message/IndividualDefendantAllFields.json";
    public static final String EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_WITHOUT_DATE_OF_BIRTH_JSON = "external/message/IndividualDefendantAllFieldsWithoutDateOfBirth.json";
    public static final String EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_PCF_JSON = "external/message/IndividualDefendantAllFieldsPCF.json";
    public static final String EXTERNAL_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS_JSON = "external/message/CorporateDefendantAllFields.json";
    public static final String EXTERNAL_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS_WITHOUT_EMAIL_JSON = "external/message/CorporateDefendantAllFieldsWithoutEmailAddress.json";

    public static final String EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_JSON = "external/message/SingleDefendantSingeOffence.json";

    public static final String EXTERNAL_MESSAGE_SINGLE_DEFENDANT_WELSH_PROSECUTION_FACTS_JSON = "external/message/SingleDefendantWelshProsecutionFacts.json";
    public static final String EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_PCF_JSON = "external/message/SingleDefendantSingleOffencePCF.json";
    public static final String EXTERNAL_MESSAGE_SINGLE_DEFENDANT_WELSH_PROSECUTION_FACTS_PCF_JSON = "external/message/SingleDefendantWelshProsecutionFactsPCF.json";

    public static final String NEW_DEFENDANTS_RECEIVED_JSON = "external/message/NewDefendantsReceived.json";
    public static final String NEW_DEFENDANTS_RECEIVED_PCF_JSON = "external/message/NewDefendantsReceivedPcf.json";
    public static final String INPUT_SPI_MESSAGE_FILE = "external/message/SingleDefendantSingeOffence.xml";
    public static final String INPUT_SPI_MESSAGE_WELSH_PROSECUTION_FACTS_FILE = "external/message/SingleDefendantWelshProsecutionFacts.xml";
    public static final String ADD_DEFENDANT_MESSAGE_FILE = "external/message/AddDefendant.xml";
    public static final String INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS = "external/message/IndividualDefendantAllfields.xml";
    public static final String INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITHOUT_DATE_OF_BIRTH = "external/message/IndividualDefendantAllfieldsWithoutDateOfBirth.xml";
    public static final String INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_WITH_NULL_ORG_NULL_DATA_CONTROLLER = "external/message/IndividualDefendantWithNullDataControllerNullOrgUnit.xml";
    public static final String INPUT_SPI_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS = "external/message/CorporateDefendantAllfields.xml";
    public static final String INPUT_SPI_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS_WITHOUT_EMAIL = "external/message/CorporateDefendantAllfieldsWithoutEmailAddress.xml";
    public static final String EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SJP_JSON = "external/message/SingleDefendantSingeOffenceSJP.json";
    public static final String EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SJP_PCF_JSON = "external/message/SingleDefendantSingleOffenceSJPPCF.json";
    public static final String EXTERNAL_MESSAGE_MULTIPLE_DEFENDANT_SJP_PCF_JSON = "external/message/SingleDefendantMultipleOffenceSJPPCF.json";

    public static final String INPUT_SPI_SJP_MESSAGE_FILE = "external/message/SingleDefendantSingeOffenceSJP.xml";

    public static final String INPUT_SPI_SJP_SINGLE_DEFENDANTS_MESSAGE_FILE_SUBSEQUENT = "external/message/SubsequentDefendantSingleOffenceSJP.xml";

    public static final String INPUT_SPI_SJP_MULTI_OFFENCES_MESSAGE_FILE = "external/message/SingleDefendantMultipleOffenceSJP.xml";
    public static final String INPUT_MULTI_OFFENCE_CC_MESSAGE_JSON = "external/message/MultiOffence.json";

    public static final String INPUT_INDIVIDUAL_AND_CORPORATE_MESSAGE_FILE = "external/message/IndividualAndCorporateDefendants.xml";
    public static final String INPUT_INDIVIDUAL_AND_CORPORATE_JSON = "external/message/IndividualAndCorporateDefendants.json";
    public static final String INPUT_SJP_INDIVIDUAL_AND_CORPORATE_PCF_JSON = "external/message/IndividualAndCorporateDefendantsPCF.json";

    public static final String INPUT_CORPORATE_CC_MESSAGE_FILE = "external/message/CorporateDefendantCC.xml";
    public static final String INPUT_CORPORATE_CC_MESSAGE_JSON = "external/message/CorporateDefendantCC.json";
    public static final String INPUT_CORPORATE_CC_MESSAGE_PCF_JSON = "external/message/CorporateDefendantCCPCF.json";


    public static final String INPUT_SPI_MDI_NO_REQUEST_ID = "external/message/errors/MDINoRequestId.xml";
    public static final String INPUT_SPI_MDI_NO_SOURCE_ID = "external/message/errors/MDINoSourceId.xml";
    public static final String INPUT_SPI_MDI_NO_DESTINATION_ID = "external/message/errors/MDINoDestinationId.xml";
    public static final String INPUT_SPI_MDI_NO_EXEC_MODE = "external/message/errors/MDINoExecMode.xml";
    public static final String INPUT_MINIMAL_SPI_MDI_MESSAGE = "external/message/errors/MDIMinimalSPIMessage.xml";

    public static final String OIValidation_MANDATORY_FIELDS = "external/message/OIValidationForMandatoryFields.xml";
    public static final String OIValidation_ERRORS_JSON = "external/message/errors/OIValidationErrors.json";
    public static final String OIValidation_ERRORS_ALL_JSON = "external/message/errors/OIValidationErrorsAll.json";
    public static final String OIValidation_302_ERROR_JSON = "external/message/errors/OIInvalid302Error.json";

    public static final String OIValidation_INVALID_FIELD_VALUES = "external/message/OIValidationWithInvalidValues.xml";
    public static final String OIValidation_MALFORMED_XML = "external/message/OIValidationMalformedXml.xml";
    public static final String ASYNC_CJSE_RESPONSE_XML = "external/message/async-response-from-cjse.xml";
    public static final String ERROR_CODE_1310 = "1310";
    private static final String PCF_COMMAND_PREFIX = "/prosecutioncasefile-service/command/api/rest/prosecutioncasefile";
    private static final String PATH_INITIATE_SJP_PROSECUTION = "/initiate-sjp-prosecution";
    private static final String PATH_CC_PROSECUTION = "/cc-prosecution";

    private JmsMessageConsumerClient stagingProseuctorsSPIPrivateEventConsumer;
    private final JmsMessageConsumerClient stagingNewDefendantsReceivedPrivateEventConsumer;
    private final JmsMessageConsumerClient stagingProseuctorsDuplicateSPIRequestPrivateEventConsumer;
    private final JmsMessageConsumerClient stagingProseuctorsErrorsReportedWithCPPResponsePrivateEventConsumer;
    private final JmsMessageConsumerClient stagingProseuctorsCJSEMessageReceivedPrivateEventConsumer;
    private final JmsMessageConsumerClient stagingProseuctorsOIValidationErrorsPrivateEventConsumer;
    private final JmsMessageConsumerClient cppMessagePreparedForSending;
    private final JmsMessageConsumerClient stagingProseuctorsMultipleDefendantErrorsReportedPrivateEventConsumer;
    private final JmsMessageConsumerClient stagingProsecutorsDifferentInitiationCodeReportedPrivateEventConsumer;
    private final JmsMessageConsumerClient stagingProseuctorsOperationalDetailsResponseReportedPrivateEventConsumer;
    private final JmsMessageConsumerClient stagingProsecutorsErrorsReportedWithUnknownCorrelationIdPrivateEventConsumer;
    private final DeadLetterQueueBrowser deadLetterQueueBrowser = new DeadLetterQueueBrowser();
    private final JmsMessageConsumerClient publicprosecutionCaseFiltered;
    private final JmsMessageConsumerClient caseFilterFailedEvent;
    private final String urn;

    public SPIInSoapHelper(final String urn) {
        this.urn = urn;
        stagingProseuctorsSPIPrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.prosecution-case-received").getMessageConsumerClient();
        stagingNewDefendantsReceivedPrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.spi-new-defendants-received").getMessageConsumerClient();
        stagingProseuctorsDuplicateSPIRequestPrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.cjse-duplicate-request-message-received").getMessageConsumerClient();
        stagingProseuctorsErrorsReportedWithCPPResponsePrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.errors-reported-with-cpp-response").getMessageConsumerClient();
        stagingProseuctorsCJSEMessageReceivedPrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.cjse-request-message-received").getMessageConsumerClient();
        stagingProseuctorsOIValidationErrorsPrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.spi-oi-validation-errors-found").getMessageConsumerClient();
        cppMessagePreparedForSending = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.cppmessage-prepared-for-sending").getMessageConsumerClient();
        stagingProseuctorsMultipleDefendantErrorsReportedPrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.prosecution-case-received-with-multiple-defendants").getMessageConsumerClient();
        stagingProsecutorsDifferentInitiationCodeReportedPrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event-case-update-received-with-different-initiation-code").getMessageConsumerClient();
        stagingProseuctorsOperationalDetailsResponseReportedPrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.operationalDetails-prepared-for-response").getMessageConsumerClient();
        stagingProsecutorsErrorsReportedWithUnknownCorrelationIdPrivateEventConsumer = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.mismatch-detected-between-correlationIds").getMessageConsumerClient();

        publicprosecutionCaseFiltered = newPublicJmsMessageConsumerClientProvider()
                .withEventNames("public.stagingprosecutorsspi.event.prosecution-case-filtered").getMessageConsumerClient();
        caseFilterFailedEvent = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames("stagingprosecutorsspi.event.case-filter-failed").getMessageConsumerClient();
    }

    public static void sendAndVerifyDuplicateSPIMessage(final String urn, final String inputResource, final String requestId, final String correlationId) throws DatatypeConfigurationException {
        SPIInSoapHelper SPIInSoapHelper = new SPIInSoapHelper(urn);
        final String spiInMessageWithURN = SPIInSoapHelper.replaceURNInSPI(readFile(inputResource)).replace("CORRELATION_ID", correlationId);
        final SubmitResponse submitResponse = SPIInSoapHelper.sendSPIInMessage(spiInMessageWithURN, requestId);
        SPIInSoapHelper.validateDuplicateCJSEEvent();
        SPISoapAdapterHelper.validateSubmitResponse(submitResponse, requestId);
    }

    public static void sendAndVerifyRetrieveRequest() {
        final RetrieveResponse retrieveResponse = SPISoapAdapterHelper.sendRetrieveRequest(new RetrieveRequest());
        SPISoapAdapterHelper.validateRetrieveResponse(retrieveResponse);
    }


    public static void sendAndVerifySPImessage(final String urn, final String inputResource, final String expectedResource, final String requestId, final String correlationId) throws IOException, DatatypeConfigurationException {
        SPIInSoapHelper SPIInSoapHelper = new SPIInSoapHelper(urn);
        final String spiInMessageWithURN = SPIInSoapHelper.replaceURNInSPI(readFile(inputResource)).replace("CORRELATION_ID", correlationId);
        final SubmitResponse submitResponse = SPIInSoapHelper.sendSPIInMessage(spiInMessageWithURN, requestId);
        SPIInSoapHelper.validateCJSEEvent();
        SPIInSoapHelper.validateProsecutionCaseReceivedEvent(expectedResource);
        SPISoapAdapterHelper.validateSubmitResponse(submitResponse, requestId);
        verifyCPPMessage(urn);
    }

    private static void verifyCPPMessage(String urn) {

        final RequestParamsBuilder requestParams = requestParams(getReadUrl("/cpp-message/" + urn), "application/vnd.stagingprosecutorsspi.query.cpp-message+json")
                .withHeader(HeaderConstants.USER_ID, QueryHelper.SYSTEM_USER_ID);
        final ResponseData responseData = poll(requestParams)
                .timeout(20L, SECONDS)
                .pollInterval(500, MILLISECONDS)
                .until(status().is(OK),
                        payload().isJson(Matchers.allOf(
                                withJsonPath("$.cppMessages[0].ptiUrn", equalTo(urn))
                        ))
                );
        assertThat(responseData.getPayload(), containsString(urn));
    }

    public static void verifyCPPMessageWithUrnPoliceSystemId(final String urn, final String payloadContent) {

        final RequestParamsBuilder requestParams = requestParams(getReadUrl("/cpp-message/" + urn), "application/vnd.stagingprosecutorsspi.query.cpp-message+json")
                .withHeader(HeaderConstants.USER_ID, QueryHelper.SYSTEM_USER_ID);
        final ResponseData responseData = poll(requestParams)
                .timeout(20L, SECONDS)
                .pollInterval(500, MILLISECONDS)
                .until(status().is(OK),
                        payload().isJson(Matchers.allOf(
                                withJsonPath("$.cppMessages[0].ptiUrn", equalTo(urn)),
                                withJsonPath("$.cppMessages[0].policeSystemId", equalTo(payloadContent))
                        ))
                );
        assertThat(responseData.getPayload(), containsString(payloadContent));
    }


    public static void sendCaseUnsupportedMessageAndVerifyAsyncErrorMessageToSpiPublished(final String urn, final UUID oiId) throws IOException, DatatypeConfigurationException {
        final String unsupportedErrorMessage = "unsupported error message";
        final String payloadForUnsupportedCasePublicEvent = Json.createObjectBuilder()
                .add("channel", "SPI")
                .add("errorMessage", unsupportedErrorMessage)
                .add("externalId", oiId.toString())
                .add("policeSystemId", "00501PoliceCaseSystem")
                .add("urn", urn)
                .build().toString();

        SPIInSoapHelper SPIInSoapHelper = new SPIInSoapHelper(urn);
        postMessageToPublicTopic(payloadForUnsupportedCasePublicEvent, "public.prosecutioncasefile.prosecution-case-unsupported");
        SPIInSoapHelper.validateOperationalDetailResponseWithGivenErrorCode(ERROR_CODE_1310, unsupportedErrorMessage);
    }

    public static void sendAndVerifyErrorWithNoRequestId(final String urn, final String inputResource, final String correlationId) {
        final SubmitResponse submitResponse = sendPayload(urn, inputResource, correlationId);
        SPISoapAdapterHelper.validateSubmitResponseCodeForInvalidRequestId(submitResponse);

    }

    public static void sendAndVerifyErrorWithInvalidRequestId(final String urn, final String inputResource, final String requestId, final String correlationId) {
        SPIInSoapHelper SPIInSoapHelper = new SPIInSoapHelper(urn);
        final String spiInMessageWithURN = SPIInSoapHelper.replaceURNInSPI(readFile(inputResource)).replace("CORRELATION_ID", correlationId)
                .replace("REQUEST_ID", requestId);
        final SubmitResponse submitResponse = SPISoapAdapterHelper.postCommand(spiInMessageWithURN);
        SPISoapAdapterHelper.validateSubmitResponseCodeForInvalidRequestId(submitResponse);
    }

    public static void sendAndVerifyErrorWithNoSourceId(final String urn, final String inputResource, final String correlationId) {
        final SubmitResponse submitResponse = sendPayload(urn, inputResource, correlationId);
        SPISoapAdapterHelper.validateSubmitResponseCodeForInvalidSourceId(submitResponse);

    }

    public static void sendAndVerifyErrorWithInvalidSourceId(final String urn, final String inputResource, final String sourceId, final String correlationId) {
        SPIInSoapHelper SPIInSoapHelper = new SPIInSoapHelper(urn);
        final String spiInMessageWithURN = SPIInSoapHelper.replaceURNInSPI(readFile(inputResource)).replace("CORRELATION_ID", correlationId)
                .replace("REQUEST_ID", randomUUID().toString())
                .replace(SubmitRequestBuilder.CMS_SOAP_ID, sourceId);
        final SubmitResponse submitResponse = SPISoapAdapterHelper.postCommand(spiInMessageWithURN);
        SPISoapAdapterHelper.validateSubmitResponseCodeForInvalidSourceId(submitResponse);
    }

    public static void sendAndVerifyErrorWithNoDestinationId(final String urn, final String inputResource, final String correlationId) {
        final SubmitResponse submitResponse = sendPayload(urn, inputResource, correlationId);
        SPISoapAdapterHelper.validateSubmitResponseCodeForInvalidDestinationId(submitResponse);

    }

    public static void sendAndVerifyErrorWithInvalidDestinationId(final String urn, final String inputResource, final String destinationId, final String correlationId) {
        SPIInSoapHelper SPIInSoapHelper = new SPIInSoapHelper(urn);
        final String spiInMessageWithURN = SPIInSoapHelper.replaceURNInSPI(readFile(inputResource)).replace("CORRELATION_ID", correlationId)
                .replace("REQUEST_ID", randomUUID().toString())
                .replace(SubmitRequestBuilder.CPP_SOAP_ID, destinationId);
        final SubmitResponse submitResponse = SPISoapAdapterHelper.postCommand(spiInMessageWithURN);

        SPISoapAdapterHelper.validateSubmitResponseCodeForInvalidDestinationId(submitResponse);
    }

    public static void sendAndVerifyErrorWithNoExecMode(final String urn, final String inputResource, final String correlationId) {
        final SubmitResponse submitResponse = sendPayload(urn, inputResource, correlationId);
        SPISoapAdapterHelper.validateSubmitResponseCodeForInvalidExecMode(submitResponse);
    }

    public static void sendAndVerifyErrorWithInvalidExecMode(final String urn, final String inputResource, final String execMode, final String correlationId) {
        SPIInSoapHelper SPIInSoapHelper = new SPIInSoapHelper(urn);
        final String spiInMessageWithURN = SPIInSoapHelper.replaceURNInSPI(readFile(inputResource)).replace("CORRELATION_ID", correlationId)
                .replace("REQUEST_ID", randomUUID().toString())
                .replace(ExecMode.ASYNCH.value(), execMode);
        final SubmitResponse submitResponse = SPISoapAdapterHelper.postCommand(spiInMessageWithURN);
        SPISoapAdapterHelper.validateSubmitResponseCodeForInvalidExecMode(submitResponse);
    }

    private static SubmitResponse sendPayload(final String urn, final String inputResource, final String correlationId) {
        SPIInSoapHelper SPIInSoapHelper = new SPIInSoapHelper(urn);
        final String spiInMessageWithURN = SPIInSoapHelper.replaceURNInSPI(readFile(inputResource)).replace("CORRELATION_ID", correlationId)
                .replace("REQUEST_ID", randomUUID().toString());
        return SPISoapAdapterHelper.postCommand(spiInMessageWithURN);
    }

    public static void verifySJPCommand(final String outputResource, final String caseUrn, final UUID oiId) {
        waitUntilCommandIssued(PATH_INITIATE_SJP_PROSECUTION, caseUrn, oiId.toString());

        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlMatching(PCF_COMMAND_PREFIX + PATH_INITIATE_SJP_PROSECUTION))
                .withRequestBody(containing(oiId.toString()))
                .withRequestBody(containing(caseUrn)));

        if (loggedRequests.isEmpty() || loggedRequests.size() > 1) {
            fail("Incorrect number of commands issued to PCF");
        }

        String outputPCFPayload = loggedRequests.get(0).getBodyAsString();

        final String expectedJsonString = readFile(outputResource).replace("[OI_ID]", oiId.toString()).replace("[URN_NUMBER]", caseUrn);

        assertEquals(expectedJsonString, outputPCFPayload, new CustomComparator(LENIENT,
                new Customization("defendants[*].id", (o1, o2) -> true),
                new Customization("defendants[*].offences[*].offenceId", (o1, o2) -> true),
                new Customization("caseDetails.caseId", (o1, o2) -> true)
        ));
    }

    public static void verifyCCCommand(String outputResource, final String caseUrn, final UUID oiId) {

        waitUntilCommandIssued(PATH_CC_PROSECUTION, caseUrn, oiId.toString());

        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlMatching(PCF_COMMAND_PREFIX + PATH_CC_PROSECUTION))
                .withRequestBody(containing(oiId.toString()))
                .withRequestBody(containing(caseUrn)));

        if (loggedRequests.isEmpty() || loggedRequests.size() > 1) {
            fail("Incorrect number of commands issued to PCF");
        }

        String outputPCFPayload = loggedRequests.get(0).getBodyAsString();

        final String expectedJsonString = readFile(outputResource).replace("[OI_ID]", oiId.toString()).replace("[URN_NUMBER]", caseUrn);

        assertEquals(expectedJsonString, outputPCFPayload, new CustomComparator(STRICT,
                new Customization("defendants[*].id", (o1, o2) -> true),
                new Customization("defendants[*].pncIdentifier", (o1, o2) -> true),
                new Customization("defendants[*].offences[*].offenceId", (o1, o2) -> true),
                new Customization("caseDetails.caseId", (o1, o2) -> true),
                new Customization("caseDetails.dateReceived", (o1, o2) -> true)
        ));

    }

    public static void sendAndVerifyErrorEvent(final String urn, final String requestId, final String inputResource, final String outputResource) throws DatatypeConfigurationException, IOException {
        SPIInSoapHelper SPIInSoapHelper = new SPIInSoapHelper(urn);
        final String spiInMessageWithURN = SPIInSoapHelper.replaceURNInSPI(readFile(inputResource));
        final SubmitRequest submitRequest = SubmitRequestBuilder
                .aSubmitRequestToCPP(requestId, spiInMessageWithURN)
                .build();
        SPISoapAdapterHelper.sendSubmitRequest(submitRequest);
        SPIInSoapHelper.validateSPIErrorEvent(outputResource);
    }

    public static void sendAndVerifyPrevioslySentError(final String urn, final String inputResource, final String outputResource, final String requestId, final String correlationId) throws DatatypeConfigurationException, IOException {
        SPIInSoapHelper SPIInSoapHelper = new SPIInSoapHelper(urn);
        final String spiInMessageWithURN = SPIInSoapHelper.replaceURNInSPI(readFile(inputResource)).replace("CORRELATION_ID", correlationId);
        SPIInSoapHelper.sendSPIInMessage(spiInMessageWithURN, requestId);

        SPIInSoapHelper.validateSPIErrorEvent(outputResource);
    }

    private static void waitUntilCommandIssued(final String commandPath, final String caseUrn, final String oiId) {
        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, MILLISECONDS)
                .pollDelay(500, MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching(PCF_COMMAND_PREFIX + commandPath))
                                .withRequestBody(containing(caseUrn))
                                .withRequestBody(containing(oiId))
                        ).size(), is(1));
    }

    private void validateSPIErrorEvent(final String outputResource) throws IOException {
        final String spiEventStr = stagingProseuctorsOIValidationErrorsPrivateEventConsumer.retrieveMessage().orElse(null);
        assertThat("SPI prosecution case oi error event not fired", spiEventStr, notNullValue());

        final JSONObject event = new JSONObject(spiEventStr);
        final JSONObject errorDetails = (JSONObject) event.get("errorDetails");

        final String expectedJsonString = readFileToString(new File(this.getClass().getClassLoader().getResource(outputResource).getFile()));
        assertEquals(replaceURNInSPI(expectedJsonString), errorDetails.toString(), STRICT);
    }

    public void validateCppMessagePreparedEvent(final String outputResource, String correlationId) throws IOException {
        final StringBuffer spiEventStr = new StringBuffer();

        Awaitility.await().timeout(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(
                        () -> {
                            spiEventStr.setLength(0);
                            spiEventStr.append(cppMessagePreparedForSending.retrieveMessage(10000L).orElse(""));
                            return spiEventStr.toString();
                        },
                        containsString(correlationId));

        final JSONObject event = new JSONObject(spiEventStr.toString());
        final JSONObject errorDetails = (JSONObject) event.get("submitRequest");

        final String expectedJsonString = readFileToString(new File(this.getClass().getClassLoader().getResource(outputResource).getFile()));
        assertEquals(replaceURNInSPI(expectedJsonString), errorDetails.toString(), new CustomComparator(LENIENT,
                new Customization("requestID", (o1, o2) -> true),
                new Customization("message", (o1, o2) -> true)
        ));
    }

    public SubmitResponse sendSPIInMessage(final String spiInMessage, final String requestId) throws DatatypeConfigurationException {
        final SubmitRequest submitRequest = SubmitRequestBuilder
                .aSubmitRequestToCPP(requestId, spiInMessage)
                .build();
        return sendSubmitRequest(submitRequest);
    }

    public void validateProsecutionCaseReceivedEvent(final String resource) throws IOException {
        final String spiEventStr = stagingProseuctorsSPIPrivateEventConsumer.retrieveMessage().orElse(null);
        assertThat("SPI prosecution case received event not fired", spiEventStr, notNullValue());

        final JSONObject event = new JSONObject(spiEventStr);
        final JSONObject policeCaseNode = (JSONObject) event.get("policeCase");

        assertThat(policeCaseNode, notNullValue());

        final String expectedJsonString = readFileToString(new File(this.getClass().getClassLoader().getResource(resource).getFile()));

        assertEquals(replaceURNInSPI(expectedJsonString), policeCaseNode.toString(), new CustomComparator(JSONCompareMode.STRICT,
                new Customization("defendants[0].id", (o1, o2) -> true)));

    }

    public void validateAddDefendantEvent(final String resource) throws IOException {
        final String spiEventStr = stagingNewDefendantsReceivedPrivateEventConsumer.retrieveMessage().orElse(null);
        assertThat("defendants added event not fired", spiEventStr, notNullValue());

        final JSONObject event = new JSONObject(spiEventStr);
        final JSONArray defendantsJsonArray = (JSONArray) event.get("defendants");

        assertThat(defendantsJsonArray, notNullValue());
        assertThat(event.get("caseId").toString(), not(isEmptyString()));

        final String expectedJsonString = readFileToString(new File(this.getClass().getClassLoader().getResource(resource).getFile()));
        assertEquals(replaceURNInSPI(expectedJsonString), defendantsJsonArray.toString(), new CustomComparator(JSONCompareMode.STRICT,
                new Customization("[0].id", (o1, o2) -> true)));

    }

    public void validateCJSEEvent() {
        final String cjseEventStr = stagingProseuctorsCJSEMessageReceivedPrivateEventConsumer.retrieveMessage().orElse(null);
        assertThat("CJSE case received event not fired", cjseEventStr, notNullValue());

        checkCJSEData(cjseEventStr);

    }

    public void validateErrorResportedForCPPResponse() {
        final String cjseEventStr = stagingProseuctorsErrorsReportedWithCPPResponsePrivateEventConsumer.retrieveMessage().orElse(null);
        assertThat("Event  not fired stagingprosecutorsspi.event.errors-reported-with-cpp-response", cjseEventStr, notNullValue());
    }

    public void validateOperationalDetailResponseWithGivenErrorCode(final String errorCode, final String errorMessage) {
        final String operationDetailsResponse = stagingProseuctorsOperationalDetailsResponseReportedPrivateEventConsumer.retrieveMessage().orElse(null);
        final JSONObject event = new JSONObject(operationDetailsResponse);
        assertThat(event, notNullValue());

        final JSONObject routeDataResponseType = (JSONObject) event.get("routeDataResponseType");
        assertThat(routeDataResponseType, notNullValue());
        assertThat(((JSONArray) routeDataResponseType.get("operationStatus")).getJSONObject(0).get("code").toString(), is(errorCode));
        assertThat(((JSONArray) routeDataResponseType.get("operationStatus")).getJSONObject(0).get("statusClass").toString(), is("FatalError"));
        assertThat(((JSONArray) routeDataResponseType.get("operationStatus")).getJSONObject(0).get("responseContext").toString(), is(errorMessage));
    }

    private void validateDuplicateCJSEEvent() {
        final String cjseDuplicateEventStr = stagingProseuctorsDuplicateSPIRequestPrivateEventConsumer.retrieveMessage().orElse(null);
        assertThat("CJSE duplicate case received event not fired", cjseDuplicateEventStr, notNullValue());

        checkCJSEData(cjseDuplicateEventStr);
    }

    private void checkCJSEData(final String cjseEvent) {
        final JSONObject spiMessageReceivedEvent = new JSONObject(cjseEvent);
        final JSONObject cjseMessage = (JSONObject) spiMessageReceivedEvent.get("cjseMessage");

        assertThat(cjseMessage, notNullValue());
        assertThat(cjseMessage.getString("sourceId"), is(CMS_SOAP_ID));
        assertThat(cjseMessage.getJSONArray("destinationID").get(0).toString(), is(CPP_SOAP_ID));
    }

    public String replaceURNInSPI(final String message) {
        return message.replace("URN_NUMBER", urn);
    }

    public void validateProsecutionCaseFilteredEvent(final String caseId) {
        final String operationDetailsResponse = publicprosecutionCaseFiltered.retrieveMessage().orElse(null);
        final JSONObject event = new JSONObject(operationDetailsResponse);
        assertThat(event.getString("caseId"), is(caseId));
        assertThat(event, notNullValue());

    }

    public void validateProsecutionCasenotFoundEvent(final String caseId) {
        final String operationDetailsResponse = caseFilterFailedEvent.retrieveMessage().orElse(null);
        final JSONObject event = new JSONObject(operationDetailsResponse);
        assertThat(event.getString("caseId"), is(caseId));
        assertThat(event, notNullValue());

    }
}
