package uk.gov.moj.cpp.staging.prosecutors.spi.it;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.ADD_DEFENDANT_MESSAGE_FILE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS_WITHOUT_EMAIL_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_PCF_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_MULTIPLE_DEFENDANT_SJP_PCF_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_PCF_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SJP_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SJP_PCF_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_SINGLE_DEFENDANT_WELSH_PROSECUTION_FACTS_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_SINGLE_DEFENDANT_WELSH_PROSECUTION_FACTS_PCF_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_CORPORATE_CC_MESSAGE_FILE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_CORPORATE_CC_MESSAGE_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_CORPORATE_CC_MESSAGE_PCF_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_INDIVIDUAL_AND_CORPORATE_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_INDIVIDUAL_AND_CORPORATE_MESSAGE_FILE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_MULTI_OFFENCE_CC_MESSAGE_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SJP_INDIVIDUAL_AND_CORPORATE_PCF_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS_WITHOUT_EMAIL;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_FILE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_WELSH_PROSECUTION_FACTS_FILE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_SJP_MESSAGE_FILE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_SJP_MULTI_OFFENCES_MESSAGE_FILE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.NEW_DEFENDANTS_RECEIVED_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.NEW_DEFENDANTS_RECEIVED_PCF_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyDuplicateSPIMessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyRetrieveRequest;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifySPImessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendCaseUnsupportedMessageAndVerifyAsyncErrorMessageToSpiPublished;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.verifyCCCommand;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.verifyCPPMessageWithUrnPoliceSystemId;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.verifySJPCommand;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.SPISoapAdapterHelper.postCommandForPoliceSystemIdUpdate;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.CJSE_SYSTEM_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.CPP_SYSTEM_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.TEST_CONFIG_INSTANCE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.stubPCFcommand;

import uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.SystemIdMapperStub;

import java.util.UUID;

import javax.json.Json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S2699"})
public class ReceiveSPIMessageIT extends AbstractIT {


    @BeforeEach
    public void setUp() {
        TEST_CONFIG_INSTANCE.setUp();
    }

    @Test
    public void sendRetrieveRequest() {
        sendAndVerifyRetrieveRequest();
    }

    @Test
    public void shouldProcessAllSpiMessagesWithoutException() throws Exception {
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_FILE, EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        final String requestId = randomUUID().toString();
        SystemIdMapperStub.stubGetRequestUUIDByMdiRequestId(requestId, randomUUID());
        final String correlationId = randomUUID().toString();
        SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId(correlationId.concat(CPP_SYSTEM_ID), randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), ADD_DEFENDANT_MESSAGE_FILE, NEW_DEFENDANTS_RECEIVED_JSON, requestId, correlationId);
    }

    @Test
    public void shouldUpdatePoliceSystemId() throws Exception {
        final String requestId = randomUUID().toString();
        SystemIdMapperStub.stubGetRequestUUIDByMdiRequestId(requestId, randomUUID());
        final String correlationId = randomUUID().toString();
        final UUID oiID = randomUUID();
        SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId(correlationId.concat(CPP_SYSTEM_ID), oiID);
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), ADD_DEFENDANT_MESSAGE_FILE, NEW_DEFENDANTS_RECEIVED_JSON, requestId, correlationId);

        final String body = Json.createObjectBuilder().add("policeSystemId", "newPoliceSystemId").build().toString();
        postCommandForPoliceSystemIdUpdate(oiID.toString(), body);
        verifyCPPMessageWithUrnPoliceSystemId(TEST_CONFIG_INSTANCE.getURN(), "newPoliceSystemId");

    }

    @Test
    public void sendDuplicateSPIInMessage() throws Exception {
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_FILE, EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        sendAndVerifyDuplicateSPIMessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_FILE, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInAndValidateIndividualDefendantWithAllFields() throws Exception {
        stubPCFcommand(randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        verifyCCCommand(EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_PCF_JSON, TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getOiId());

        final String requestId = randomUUID().toString();
        SystemIdMapperStub.stubGetRequestUUIDByMdiRequestId(requestId, randomUUID());
        final String correlationId = randomUUID().toString();
        SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId(correlationId.concat(CPP_SYSTEM_ID), randomUUID());

        // prepare mappings for sending 2nd message for same case
        final UUID oiIdForSecondMessage = randomUUID();

        SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId(correlationId.concat(CPP_SYSTEM_ID), oiIdForSecondMessage);
        SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId(correlationId.concat(CJSE_SYSTEM_ID), oiIdForSecondMessage);
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), ADD_DEFENDANT_MESSAGE_FILE, NEW_DEFENDANTS_RECEIVED_JSON, requestId, correlationId);
        verifyCCCommand(NEW_DEFENDANTS_RECEIVED_PCF_JSON, TEST_CONFIG_INSTANCE.getURN(), oiIdForSecondMessage);
    }

    @Test
    public void sendSPIInAndValidateCorporateDefendantWithAllFields() throws Exception {
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInAndValidateCorporateDefendantWithoutEmailAddress() throws Exception {
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS_WITHOUT_EMAIL, EXTERNAL_MESSAGE_CORPORATE_DEFENDANT_ALL_FIELDS_WITHOUT_EMAIL_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInAndValidateMessageWithSjpCaseInitiationCode() throws Exception {
        stubPCFcommand(randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_SJP_MESSAGE_FILE, EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SJP_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        verifySJPCommand(EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SJP_PCF_JSON, TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getOiId());

        final String requestId = randomUUID().toString();
        SystemIdMapperStub.stubGetRequestUUIDByMdiRequestId(requestId, randomUUID());
        final String correlationId = randomUUID().toString();
        SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId(correlationId.concat(CPP_SYSTEM_ID), randomUUID());

        // prepare mappings for sending 2nd message for same case
        final UUID oiIdForSecondMessage = randomUUID();
        SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId(correlationId.concat(CPP_SYSTEM_ID), oiIdForSecondMessage);
        SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId(correlationId.concat(CJSE_SYSTEM_ID), oiIdForSecondMessage);
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_SJP_MESSAGE_FILE, EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SJP_JSON, requestId, correlationId);
        verifySJPCommand(EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SJP_PCF_JSON, TEST_CONFIG_INSTANCE.getURN(), oiIdForSecondMessage);
    }

    @Test
    public void sendSPIInAndValidateMessageWithCCProsecution() throws Exception {
        stubPCFcommand(randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_FILE, EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        verifyCCCommand(EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_PCF_JSON, TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getOiId());
    }

    @Test
    public void shouldProcessNewSPIMessageAndAddDefendantMessageWithWelshProsecutionFacts() throws Exception {
        stubPCFcommand(randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_WELSH_PROSECUTION_FACTS_FILE, EXTERNAL_MESSAGE_SINGLE_DEFENDANT_WELSH_PROSECUTION_FACTS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        verifyCCCommand(EXTERNAL_MESSAGE_SINGLE_DEFENDANT_WELSH_PROSECUTION_FACTS_PCF_JSON, TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getOiId());
    }

    @Test
    public void sendSPIAndValidateMessageBothIndividualAndCorporateDefendants() throws Exception {
        stubPCFcommand(randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_INDIVIDUAL_AND_CORPORATE_MESSAGE_FILE, INPUT_INDIVIDUAL_AND_CORPORATE_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        verifySJPCommand(INPUT_SJP_INDIVIDUAL_AND_CORPORATE_PCF_JSON, TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getOiId());
    }

    @Test
    public void sendSPIAndValidateCorporateDefendantCCCase() throws Exception {
        stubPCFcommand(randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_CORPORATE_CC_MESSAGE_FILE, INPUT_CORPORATE_CC_MESSAGE_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        verifyCCCommand(INPUT_CORPORATE_CC_MESSAGE_PCF_JSON, TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getOiId());
    }

    @Test
    public void sendSPIMultipleOffencesAndValidateCase() throws Exception {
        stubPCFcommand(randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_SJP_MULTI_OFFENCES_MESSAGE_FILE, INPUT_MULTI_OFFENCE_CC_MESSAGE_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        verifySJPCommand(EXTERNAL_MESSAGE_MULTIPLE_DEFENDANT_SJP_PCF_JSON, TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getOiId());
    }

    @Test
    public void sendDuplicateSPIInMessageForSJP() throws Exception {
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_FILE, EXTERNAL_MESSAGE_SINGLE_DEFENDANT_SINGE_OFFENCE_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        sendAndVerifyDuplicateSPIMessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_FILE, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void testCaseUnsupported_whenCaseUnsupportedPublicEventIsRaised_expectSPIPoliceCaseUnsupportedPublicEvent() throws Exception {

        stubPCFcommand(randomUUID());
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_CORPORATE_CC_MESSAGE_FILE, INPUT_CORPORATE_CC_MESSAGE_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        verifyCCCommand(INPUT_CORPORATE_CC_MESSAGE_PCF_JSON, TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getOiId());

        sendCaseUnsupportedMessageAndVerifyAsyncErrorMessageToSpiPublished(TEST_CONFIG_INSTANCE.getURN(), TEST_CONFIG_INSTANCE.getOiId());
    }

}

