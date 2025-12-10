package uk.gov.moj.cpp.staging.command.handler;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveRetryDelayTimerFinished;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSyncResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.aggregate.CPPMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SyncResponse;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingSyncResponseHandlerTest {

    private static final String RECEIVE_SYNC_RESPONSE_COMMAND = "stagingprosecutorsspi.command.handler.receive-sync-response";
    private static final String RECEIVE_RETRY_DELAY_TIMER_COMMAND = "stagingprosecutorsspi.command.handler.receive-retry-delay-timer-finished";

    @InjectMocks
    private StagingSyncResponseHandler stagingSyncResponseHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    EventStream eventStream;

    @Mock
    CPPMessage cppMessage;

    @Mock
    private AggregateService aggregateService;

    @Mock
    Stream<Object> newEvents;

    private int retryLimitInteger = 0;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptorStream;

    @Mock
    private Stream<Object> mappedNewEvents;

    @Test
    public void receiveSpiCjseMessageTest() throws EventStreamException {

        final ReceiveSyncResponse receiveSyncResponse = getMockReceiveSyncResponse();
        final Envelope<ReceiveSyncResponse> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(RECEIVE_SYNC_RESPONSE_COMMAND), receiveSyncResponse);
        when(eventSource.getStreamById(UUID.fromString(receiveSyncResponse.getSyncResponse().getRequestID()))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CPPMessage.class)).thenReturn(cppMessage);
        when(cppMessage.receiveSyncResponse(receiveSyncResponse.getSyncResponse(), retryLimitInteger)).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingSyncResponseHandler.receiveSpiCjseMessage(envelope);
        checkEventsAppendedAreMappedNewEvents();
    }



    @Test
    public void receiveDelayTimerFinishedTest() throws EventStreamException {
        final ReceiveRetryDelayTimerFinished receiveRetryDelayTimerFinished = getMockReceiveRetryDelayTimerFinished();
        final Envelope<ReceiveRetryDelayTimerFinished> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(RECEIVE_RETRY_DELAY_TIMER_COMMAND), receiveRetryDelayTimerFinished);
        when(eventSource.getStreamById(receiveRetryDelayTimerFinished.getId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CPPMessage.class)).thenReturn(cppMessage);
        when(cppMessage.sendMessageForRetry(retryLimitInteger)).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingSyncResponseHandler.receiveDelayTimerFinished(envelope);
        checkEventsAppendedAreMappedNewEvents();
    }

    private ReceiveSyncResponse getMockReceiveSyncResponse() {
        return ReceiveSyncResponse.receiveSyncResponse()
                .withSyncResponse(SyncResponse.syncResponse()
                        .withRequestID(UUID.randomUUID().toString())
                        .build())
                .build();

    }

    private ReceiveRetryDelayTimerFinished getMockReceiveRetryDelayTimerFinished() {
        return ReceiveRetryDelayTimerFinished.receiveRetryDelayTimerFinished().withId(UUID.randomUUID()).build();


    }

    private void checkEventsAppendedAreMappedNewEvents() throws EventStreamException {
        verify(eventStream).append(argumentCaptorStream.capture());
        final Stream<JsonEnvelope> stream = argumentCaptorStream.getValue();
        assertThat(stream, is(mappedNewEvents));
    }
}