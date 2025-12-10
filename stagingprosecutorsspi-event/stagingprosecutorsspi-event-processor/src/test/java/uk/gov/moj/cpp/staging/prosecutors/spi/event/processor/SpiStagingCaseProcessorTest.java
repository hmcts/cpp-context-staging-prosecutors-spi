package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.processor.SpiStagingCaseProcessor.OI_RECEIVE_RESPONSE_COMMAND;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.processor.SpiStagingCaseProcessor.OI_REQUEST_RECEIVE_COMMAND;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.processor.SpiStagingCaseProcessor.SPI_COMMAND_RECEIVE_CASE_COMMAND;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.ReceiveProsecutionCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OiRequestMessageReceived;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseRequestMessageReceived;
import uk.gov.moj.cpp.staging.soap.schema.OIDetails;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import javax.xml.bind.JAXBException;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.SAXException;

@ExtendWith(MockitoExtension.class)
public class SpiStagingCaseProcessorTest {

    private static final String POLICE_SYSTEM_ID = "00301PoliceCaseSystem";
    @InjectMocks
    private SpiStagingCaseProcessor spiStagingCaseProcessor;
    @Mock
    private Sender sender;
    @Mock
    private Envelope<CjseRequestMessageReceived> cjseRequestMessageReceivedEnvelope;
    @Mock
    private Envelope<OiRequestMessageReceived> oiRequestMessageReceivedEnvelope;
    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;


    public SpiStagingCaseProcessorTest() throws JAXBException {
    }

    public static String readFile(final String path) throws IOException {
        return Resources.toString(
                Resources.getResource(path),
                Charset.defaultCharset()
        );
    }

    @Test
    public void shouldSendPoliceCaseWithSPIProsecutionCaseCommand() throws IOException, JAXBException, SAXException {
        final Metadata metadata = getMetaData();
        final OiRequestMessageReceived oiRequestMessageReceived = getMockOIRequest(readFile("PoliceCase.xml"));

        when(oiRequestMessageReceivedEnvelope.metadata()).thenReturn(metadata);
        when(oiRequestMessageReceivedEnvelope.payload()).thenReturn(oiRequestMessageReceived);
        spiStagingCaseProcessor.onOiRequestReceived(oiRequestMessageReceivedEnvelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ReceiveProsecutionCase> captorValue = envelopeArgumentCaptor.getValue();

        assertThat(captorValue.payload(), is(notNullValue()));
        assertThat(captorValue.payload().getPoliceCase().getCaseDetails().getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(captorValue.metadata().name(), is(SPI_COMMAND_RECEIVE_CASE_COMMAND));
    }

    private OiRequestMessageReceived getMockOIRequest(final String oiMessageContent) throws IOException {

        return OiRequestMessageReceived.oiRequestMessageReceived()
                .withCorrelationId(randomUUID().toString())
                .withDataStreamContent(oiMessageContent)
                .withOiId(randomUUID())
                .withSystemId(randomUUID().toString())
                .withPoliceSystemId(POLICE_SYSTEM_ID)
                .build();
    }


    @Test
    public void shouldSendPoliceCaseWithSPIOIReceiveCommand() throws JAXBException, SAXException, IOException {
        final Metadata metadata = getMetaData();
        final CjseRequestMessageReceived cjseRequestMessageReceived = getMockCjseRequestMessageReceived(readFile("IndividualDefendantAllfields.xml"));
        when(cjseRequestMessageReceivedEnvelope.metadata()).thenReturn(metadata);
        when(cjseRequestMessageReceivedEnvelope.payload()).thenReturn(cjseRequestMessageReceived);
        spiStagingCaseProcessor.onCjseRequestMessageReceived(cjseRequestMessageReceivedEnvelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<OIDetails> captorValue = envelopeArgumentCaptor.getValue();

        assertThat(captorValue.payload(), is(notNullValue()));
        assertThat(captorValue.metadata().name(), is(OI_REQUEST_RECEIVE_COMMAND));
    }

    @Test
    public void shouldReceivePoliceCaseResponseWithSPIOIResponseReceiveCommand() throws JAXBException, SAXException, IOException {
        final Metadata metadata = getMetaData();
        final CjseRequestMessageReceived cjseRequestMessageReceived = getMockCjseRequestMessageReceived(readFile("response-from-cjse.xml"));
        when(cjseRequestMessageReceivedEnvelope.metadata()).thenReturn(metadata);
        when(cjseRequestMessageReceivedEnvelope.payload()).thenReturn(cjseRequestMessageReceived);
        spiStagingCaseProcessor.onCjseRequestMessageReceived(cjseRequestMessageReceivedEnvelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<OIDetails> captorValue = envelopeArgumentCaptor.getValue();

        assertThat(captorValue.payload(), is(notNullValue()));
        assertThat(captorValue.metadata().name(), is(OI_RECEIVE_RESPONSE_COMMAND));
    }

    private Metadata getMetaData() {
        return DefaultJsonMetadata.metadataBuilder()
                .createdAt(ZonedDateTime.now())
                .withCausation(randomUUID())
                .withId(randomUUID())
                .withName("COMMAND")
                .build();
    }

    private CjseRequestMessageReceived getMockCjseRequestMessageReceived(final String payload) throws IOException {
        final CjseMessage cjseMessage = getMockCjseMessageRequest(payload);

        return CjseRequestMessageReceived.cjseRequestMessageReceived()
                .withCjseMessage(cjseMessage)
                .build();
    }

    private CjseMessage getMockCjseMessageRequest(String cjseMessageContent) throws IOException {

        return CjseMessage.cjseMessage()
                .withSourceId("sourceId")
                .withExecMode("Async")
                .withDestinationID(new ArrayList<>())
                .withMessage(cjseMessageContent)
                .withTimestamp("2016-09-08T16:11:24.436Z")
                .build();
    }

}
