package uk.gov.moj.cpp.staging.command.handler;


import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.staging.command.util.EventStreamAppender.appendMetaDataInEventStream;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.command.service.SystemMapperService;
import uk.gov.moj.cpp.staging.prosecutors.spi.aggregate.OIMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.SendSpiResult;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiResult;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class StagingResultHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private SystemMapperService systemMapperService;

    @Inject
    @Value(key = "cpp.system.id", defaultValue = "C00CommonPlatform")
    private String cppSystemId;

    @Handles("stagingprosecutorsspi.command.handler.send-spi-result")
    public void handleSendSpiResultCommand(final Envelope<SendSpiResult> envelope) throws EventStreamException {
        final SendSpiResult sendSpiResult = envelope.payload();
        final SpiResult spiResult = sendSpiResult.getSpiResult();
        final String ptiUrn = spiResult.getPtiUrn();
        final UUID spiResultId = systemMapperService.getSpiResultIdForURN(ptiUrn);

        final UUID correlationId = randomUUID();
        final UUID oiId = systemMapperService.getOiIdForCorrelationAndSystemId(correlationId.toString(), cppSystemId);
        final EventStream eventStream = eventSource.getStreamById(oiId);
        final OIMessage oiMessage = aggregateService.get(eventStream, OIMessage.class);
        final Stream<Object> newEvents = oiMessage.prepareSPIResult(spiResultId, spiResult, cppSystemId, correlationId);
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }


}
