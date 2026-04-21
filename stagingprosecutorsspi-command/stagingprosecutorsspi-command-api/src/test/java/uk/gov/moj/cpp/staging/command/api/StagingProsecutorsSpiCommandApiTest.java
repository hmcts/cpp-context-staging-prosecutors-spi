package uk.gov.moj.cpp.staging.command.api;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;


@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsSpiCommandApiTest {

    private static final String RECEIVE_PROSECUTION_CASE_COMMAND_NAME = "stagingprosecutorsspi.command.handler.receive-prosecution-case";
    private static final String FILTER_PROSECUTION_CASE_COMMAND_NAME = "stagingprosecutorsspi.command.handler.filter-prosecution-case";
    private static final String SPI_OI_UPDATE_POLICE_SYSTEM_ID = "stagingprosecutorsspi.command.handler.spi-oi-update-police-system-id";

    @InjectMocks
    private StagingProsecutorsSpiCommandApi stagingProsecutorsSpiCommandApi;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    private JsonEnvelope jsonEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @BeforeEach
    public void setup() {
        jsonEnvelope = getMockJsonEnvelope();
    }

    private JsonEnvelope getMockJsonEnvelope() {
        return createEnvelope(RECEIVE_PROSECUTION_CASE_COMMAND_NAME,
                createObjectBuilder()
                        .add("Payload1Key", "Payload1Value")
                        .add("Payload2Key", "Payload2Value")
                        .add("Payload3Key", "Payload3Value")
                        .add("message","<RouteData/>")
                        .build()
        );
    }

    @Test
    public void shouldSendCorrectEnvelopeForCPPMessage() {
        stagingProsecutorsSpiCommandApi.resendCPPMessage(jsonEnvelope);
        checkEnvelopeHasCorrectMetadataAndPayload();
    }

    @Test
    public void shouldSendCorrectEnvelopeForPoliceCase() {
        stagingProsecutorsSpiCommandApi.spiReceivePoliceCase(jsonEnvelope);
        checkEnvelopeHasCorrectMetadataAndPayload();
    }

    @Test
    public void shouldSendCorrectEnvelopeForCJSEMessage() throws Exception {
        stagingProsecutorsSpiCommandApi.receiveCJSEMessage(jsonEnvelope);
        checkEnvelopeHasCorrectMetadataAndPayload();
    }

    @Test
    public void shouldSendCorrectEnvelopeForCaseFilter() {
        stagingProsecutorsSpiCommandApi.spiFilterProsecutionCase(jsonEnvelope);
        checkEnvelopeHasCorrectMetadataAndPayloadForCaseFilter();
    }

    private void checkEnvelopeHasCorrectMetadataAndPayload() {
        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope envelope = envelopeArgumentCaptor.getValue();

        assertThat(envelope.metadata().name(), is(RECEIVE_PROSECUTION_CASE_COMMAND_NAME));
        assertThat(envelope.payload(), is(jsonEnvelope.payload()));
    }

    private void checkEnvelopeHasCorrectMetadataAndPayloadForCaseFilter() {
        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope envelope = envelopeArgumentCaptor.getValue();

        assertThat(envelope.metadata().name(), is(FILTER_PROSECUTION_CASE_COMMAND_NAME));
        assertThat(envelope.payload(), is(jsonEnvelope.payload()));
    }

    @Test
    public void shouldSendCorrectEnvelopeForUpdatePoliceSystem() {
        stagingProsecutorsSpiCommandApi.updatePoliceSystem(jsonEnvelope);
        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope envelope = envelopeArgumentCaptor.getValue();
        assertEquals(SPI_OI_UPDATE_POLICE_SYSTEM_ID, envelope.metadata().name());
    }


}
