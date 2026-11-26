package uk.gov.moj.cpp.staging.command.handler;


import static java.lang.Integer.parseInt;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.staging.command.util.EventStreamAppender.appendMetaDataInEventStream;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveRetryDelayTimerFinished;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSyncResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.aggregate.CPPMessage;

import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class StagingSyncResponseHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    @Value(key = "cpp.message.retry.limit", defaultValue = "5")
    private String retryLimit;

    private int retryLimitInteger;


    @PostConstruct
    public void setUp(){
        retryLimitInteger = parseInt(retryLimit);
    }

    @Handles("stagingprosecutorsspi.command.handler.receive-sync-response")
    public void receiveSpiCjseMessage(final Envelope<ReceiveSyncResponse> envelope) throws EventStreamException {
        final ReceiveSyncResponse receiveSyncResponse = envelope.payload();
        final UUID cppMessageId = fromString(receiveSyncResponse.getSyncResponse().getRequestID());
        final EventStream eventStream = eventSource.getStreamById(cppMessageId);
        final CPPMessage cppMessage = aggregateService.get(eventStream, CPPMessage.class);
        final Stream<Object> newEvents = cppMessage.receiveSyncResponse(receiveSyncResponse.getSyncResponse(), retryLimitInteger);
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }

    @Handles("stagingprosecutorsspi.command.handler.receive-retry-delay-timer-finished")
    public void receiveDelayTimerFinished(final Envelope<ReceiveRetryDelayTimerFinished> envelope) throws EventStreamException {
        final UUID cppMessageId =  envelope.payload().getId();
        final EventStream eventStream = eventSource.getStreamById(cppMessageId);
        final CPPMessage cppMessage = aggregateService.get(eventStream, CPPMessage.class);
        final Stream<Object> newEvents = cppMessage.sendMessageForRetry(retryLimitInteger);
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }
    //



}
