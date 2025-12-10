package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.cjse.schemas.endpoint.types.SubmitResponse;
import uk.gov.cjse.schemas.endpoint.wsdl.CJSEPort;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSyncResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service.ActivitiService;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.CPPMessagePreparedForSending;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.RetryDelayRequired;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.xml.ws.BindingProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CPPMessagePreparedProcessorTest {

    private static final String SPI_COMMAND_RECIEVE_SYNC_RESPONSE = "stagingprosecutorsspi.command.handler.receive-sync-response";

    @Mock(extraInterfaces = BindingProvider.class)
    private CJSEPort cjsePort;

    @Mock
    private Sender sender;

    @Mock
    private Envelope<CPPMessagePreparedForSending> envelope;
    @Mock
    private CPPMessagePreparedForSending cppMessagePreparedForSending;
    @Mock
    private SubmitResponse submitResponse;

    @Mock
    private SubmitRequest submitRequest;
    @Captor
    private ArgumentCaptor<Envelope<ReceiveSyncResponse>> envelopeArgumentCaptor;

    @Mock
    private Envelope<RetryDelayRequired> envelopeCppmessageRetryDelayRequired;

    @Mock
    private RetryDelayRequired cppmessageRetryDelayRequired;

    @Mock
    private ActivitiService activitiService;

    @InjectMocks
    private CPPMessagePreparedProcessor cppMessagePreparedProcessor;

    @Test
    public void processCPPMessagePrepared() throws Exception {
        when(envelope.payload()).thenReturn(cppMessagePreparedForSending);
        when(cppMessagePreparedForSending.getSubmitRequest()).thenReturn(submitRequest);
        when(cjsePort.submit(submitRequest)).thenReturn(submitResponse);
        when(envelope.metadata()).thenReturn(getMetaData());
        when(submitResponse.getResponseCode()).thenReturn(123);
        when(submitResponse.getResponseText()).thenReturn("ResText");

        cppMessagePreparedProcessor.processCPPMessagePrepared(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ReceiveSyncResponse> captorValue = envelopeArgumentCaptor.getValue();

        assertThat(captorValue.payload(), is(notNullValue()));
        assertThat(captorValue.metadata().name(), is(SPI_COMMAND_RECIEVE_SYNC_RESPONSE));
    }

    @Test
    public void processTimerDelayRequired() throws Exception {
        final String requestId = UUID.randomUUID().toString();
        Metadata metadata = getMetaData();
        when(envelopeCppmessageRetryDelayRequired.payload()).thenReturn(cppmessageRetryDelayRequired);
        when(cppmessageRetryDelayRequired.getRequestId()).thenReturn(requestId);
        when(envelopeCppmessageRetryDelayRequired.metadata()).thenReturn(metadata);

        cppMessagePreparedProcessor.processTimerDelayRequired(envelopeCppmessageRetryDelayRequired);

        verify(activitiService).startTimerProcess(fromString(requestId), metadata);
    }

    private Metadata getMetaData() {
        return DefaultJsonMetadata.metadataBuilder()
                .createdAt(ZonedDateTime.now())
                .withCausation(randomUUID())
                .withName(SPI_COMMAND_RECIEVE_SYNC_RESPONSE)
                .withId(randomUUID())
                .build();
    }

}