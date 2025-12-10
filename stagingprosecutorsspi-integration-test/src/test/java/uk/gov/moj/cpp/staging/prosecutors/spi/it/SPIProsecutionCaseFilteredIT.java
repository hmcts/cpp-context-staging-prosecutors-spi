package uk.gov.moj.cpp.staging.prosecutors.spi.it;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifySPImessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.TEST_CONFIG_INSTANCE;

import uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper;
import uk.gov.moj.cpp.staging.prosecutors.spi.utils.SPISoapAdapterHelper;

import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SPIProsecutionCaseFilteredIT extends AbstractIT {

    private static final String FILTER_PROSECUTION_CASE_MEDIA_TYPE = "application/vnd.stagingprosecutorsspi.command.spi.filter-prosecution-case+json";

    @BeforeEach
    public void setUp() {
        TEST_CONFIG_INSTANCE.setUp();
    }

    @Test
    public void shouldPublishPublicEventWhenCaseIsFiltered() {
        final String body = "{}";
        final SPIInSoapHelper spiInSoapHelper = new SPIInSoapHelper(TEST_CONFIG_INSTANCE.getURN());
        final Response response = SPISoapAdapterHelper.postCommandForCaseFilter(TEST_CONFIG_INSTANCE.getURN(), body, FILTER_PROSECUTION_CASE_MEDIA_TYPE);
        assertThat(response.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        spiInSoapHelper.validateProsecutionCaseFilteredEvent(TEST_CONFIG_INSTANCE.getCaseId().toString());

    }

    @Test
    public void shouldRaiseFilterFailedEventWhenCaseAlreadyExists() throws IOException, DatatypeConfigurationException {
        String body = "{}";
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());
        SPIInSoapHelper spiInSoapHelper = new SPIInSoapHelper(TEST_CONFIG_INSTANCE.getURN());
        final Response response = SPISoapAdapterHelper.postCommandForCaseFilter(TEST_CONFIG_INSTANCE.getURN(), body, FILTER_PROSECUTION_CASE_MEDIA_TYPE);
        assertThat(response.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
        spiInSoapHelper.validateProsecutionCasenotFoundEvent(TEST_CONFIG_INSTANCE.getCaseId().toString());

    }
}
