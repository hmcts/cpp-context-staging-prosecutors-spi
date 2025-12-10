package uk.gov.moj.cpp.staging.prosecutors.spi.it;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_MINIMAL_SPI_MDI_MESSAGE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MDI_NO_DESTINATION_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MDI_NO_EXEC_MODE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MDI_NO_REQUEST_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MDI_NO_SOURCE_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyErrorWithInvalidDestinationId;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyErrorWithInvalidExecMode;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyErrorWithInvalidRequestId;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyErrorWithInvalidSourceId;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyErrorWithNoDestinationId;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyErrorWithNoExecMode;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyErrorWithNoRequestId;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifyErrorWithNoSourceId;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.TEST_CONFIG_INSTANCE;

import uk.gov.moj.cpp.staging.prosecutors.spi.utils.SubmitRequestBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MDIErrorsIT extends AbstractIT {

    @BeforeEach
    public void setUp() {
        TEST_CONFIG_INSTANCE.setUp();
    }

    @Test
    public void sendSPIInWithMDIValidationErrorNoRequestId() {
        sendAndVerifyErrorWithNoRequestId(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MDI_NO_REQUEST_ID, TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorRequestIdWithSpaces() {
        sendAndVerifyErrorWithInvalidRequestId(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "  " + randomUUID().toString(), TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorEmptyRequestId() {
        sendAndVerifyErrorWithInvalidRequestId(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "", TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorNoSourceId() {
        sendAndVerifyErrorWithNoSourceId(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MDI_NO_SOURCE_ID, TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorSourceIdWithSpaces() {
        sendAndVerifyErrorWithInvalidSourceId(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "  " + SubmitRequestBuilder.CMS_SOAP_ID, TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorEmptySourceId() {
        sendAndVerifyErrorWithInvalidSourceId(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "", TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorInvalidSourceId() {
        sendAndVerifyErrorWithInvalidSourceId(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "invalidSourceId", TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorNoDestinationId() {
        sendAndVerifyErrorWithNoDestinationId(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MDI_NO_DESTINATION_ID, TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorDestinationIdWithSpaces() {
        sendAndVerifyErrorWithInvalidDestinationId(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "   " + SubmitRequestBuilder.CPP_SOAP_ID, TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorEmptyDestinationId() {
        sendAndVerifyErrorWithInvalidDestinationId(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "", TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorInvalidDestinationId() {
        sendAndVerifyErrorWithInvalidDestinationId(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "invalidDestinationId", TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorNoExecMode() {
        sendAndVerifyErrorWithNoExecMode(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MDI_NO_EXEC_MODE, TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorExecModeWithSpaces() {
        sendAndVerifyErrorWithInvalidExecMode(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "  ASYNCH", TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorEmptyExecMode() {
        sendAndVerifyErrorWithInvalidExecMode(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "", TEST_CONFIG_INSTANCE.getCorrelationId());
    }

    @Test
    public void sendSPIInWithMDIValidationErrorInvalidExecMode() {
        sendAndVerifyErrorWithInvalidExecMode(TEST_CONFIG_INSTANCE.getURN(), INPUT_MINIMAL_SPI_MDI_MESSAGE, "SYNCH", TEST_CONFIG_INSTANCE.getCorrelationId());
    }


}
