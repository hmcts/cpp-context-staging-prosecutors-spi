package uk.gov.moj.cpp.staging.command.handler;

import uk.gov.cjse.schemas.common.operations.SystemDetailsStructure;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.staging.prosecutors.json.schemas.SpiProsecutioncaseEjected;
import uk.gov.moj.cpp.staging.command.service.SystemMapperService;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MdiIdWithMessage;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.PrepareAsyncErrorResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.aggregate.CJSEMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.aggregate.CPPMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.aggregate.OIMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.aggregate.SPIPoliceCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.FilterProsecutionCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.ReceiveProsecutionCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.SpiOiUpdatePoliceSystemId;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.ResendMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.OIValidator;
import uk.gov.moj.cpp.staging.soap.schema.OIDetails;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.staging.command.util.EventStreamAppender.appendMetaDataInEventStream;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(COMMAND_HANDLER)
public class StagingProsecutorsSpiCommandHandler {

    @Inject
    private SystemMapperService systemMapperService;
    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private OIValidator oiValidator;

    @Inject
    @Value(key = "cjse.system.id", defaultValue = "Z00CJSE")
    private String cjseSystemId;

    @Inject
    @Value(key = "cpp.system.id", defaultValue = "C00CommonPlatform")
    private String cppSystemId;


    @Handles("hmcts.cjs.receive-spi-message")
    public void receiveSpiCjseMessage(final Envelope<CjseMessage> envelope) throws EventStreamException {
        final CjseMessage cjseMessagePayload = envelope.payload();
        final UUID cjseRequestId = systemMapperService.getMappedUUIDForRequestId(Objects.toString(cjseMessagePayload.getRequestId(), "INVALID_REQUEST_ID"));
        final EventStream eventStream = eventSource.getStreamById(cjseRequestId);
        final CJSEMessage cjseMessageAggregate = aggregateService.get(eventStream, CJSEMessage.class);
        final Stream<Object> newEvents = cjseMessageAggregate.receiveCjseMessage(cjseMessagePayload);
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }

    @Handles("hmcts.cjs.resend-message")
    public void resendCPPMessage(final Envelope<ResendMessage> envelope) throws EventStreamException {
        final ResendMessage resendMessage = envelope.payload();
        final UUID cppMessageId = resendMessage.getId();

        final EventStream eventStream = eventSource.getStreamById(cppMessageId);
        final CPPMessage cppMessage = aggregateService.get(eventStream, CPPMessage.class);

        final Stream<Object> newEvents = cppMessage.resendMessage(cppMessageId);
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }

    @Handles("stagingprosecutorsspi.command.handler.spi-oi-receive")
    public void receiveSpiOiRequestMessage(final Envelope<OIDetails> envelope) throws EventStreamException {
        final OIDetails routeDataRequestTypePayload = envelope.payload();
        final SystemDetailsStructure requestFromSystem = routeDataRequestTypePayload.getRouteDataRequestType().getRequestFromSystem();
        final UUID oiId = systemMapperService.getOiIdForCorrelationAndSystemId(requestFromSystem.getCorrelationID(), requestFromSystem.getSystemID().getValue());
        final EventStream eventStream = eventSource.getStreamById(oiId);
        final OIMessage oiMessage = aggregateService.get(eventStream, OIMessage.class);
        final Stream<Object> newEvents = oiMessage.oiRequestReceived(routeDataRequestTypePayload, oiId, oiValidator);
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }

    @Handles("stagingprosecutorsspi.command.handler.receive-prosecution-case")
    public void processProsecutionCase(final Envelope<ReceiveProsecutionCase> envelope) throws EventStreamException {
        final PoliceCase policeCase = envelope.payload().getPoliceCase();
        final String ptiUrn = policeCase.getCaseDetails().getPtiurn();
        final UUID caseId = systemMapperService.getCaseIdForPtiURNAndOriginatingOrganisation(ptiUrn);
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final SPIPoliceCase spiPoliceCase = aggregateService.get(eventStream, SPIPoliceCase.class);
        final Stream<Object> newEvents = spiPoliceCase.receivePoliceCase(caseId, envelope.payload().getOiId(), policeCase);
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }

    @Handles("stagingprosecutorsspi.command.handler.prepare-cpp-message-for-sending")
    public void prepareCPPMessage(final Envelope<MdiIdWithMessage> envelope) throws EventStreamException {
        final MdiIdWithMessage mdiIdWithMessage = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(mdiIdWithMessage.getId());
        final CPPMessage cppMessage = aggregateService.get(eventStream, CPPMessage.class);
        final Stream<Object> newEvents = cppMessage.prepareCPPMessageForSending(mdiIdWithMessage.getId().toString(), mdiIdWithMessage.getMessage(), mdiIdWithMessage.getSystemId(), cjseSystemId);
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }

    @Handles("stagingprosecutorsspi.command.handler.prepare-async-error-response")
    public void prepareAsyncErrorResponse(final Envelope<PrepareAsyncErrorResponse> envelope) throws EventStreamException {
        final PrepareAsyncErrorResponse prepareAsyncErrorResponse = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(prepareAsyncErrorResponse.getOiId());
        final OIMessage oiMessage = aggregateService.get(eventStream, OIMessage.class);
        final Stream<Object> newEvents = oiMessage.prepareOIResponseForXSDFailures(prepareAsyncErrorResponse.getErrorMessage());
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }

    @Handles("stagingprosecutorsspi.command.handler.spi-oi-receive_response")
    public void receiveOiResponseMessage(final Envelope<OIDetails> envelope) throws EventStreamException {
        final OIDetails oiDetails = envelope.payload();
        final SystemDetailsStructure systemDetailsStructure = oiDetails.getRouteDataResponseType().getResponseToSystem();
        UUID oiStreamId;
        if (systemDetailsStructure.getSystemID().getValue().equals(cjseSystemId) || systemDetailsStructure.getSystemID().getValue().equals(cppSystemId)) {
            oiStreamId = systemMapperService.getOiIdForCorrelationAndSystemId(systemDetailsStructure.getCorrelationID(), systemDetailsStructure.getSystemID().getValue());
        } else {
            oiStreamId = fromString(oiDetails.getRouteDataResponseType().getResponseToSystem().getCorrelationID());
        }
        final EventStream eventStream = eventSource.getStreamById(oiStreamId);
        final OIMessage oiMessage = aggregateService.get(eventStream, OIMessage.class);
        final Stream<Object> newEvents = oiMessage.oiResponseReceived(oiDetails);
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }

    @Handles("stagingprosecutorsspi.command.handler.spi-prosecutioncase-ejected")
    public void handleEjectedSPICase(final Envelope<SpiProsecutioncaseEjected> envelope) throws EventStreamException {
        final SpiProsecutioncaseEjected spiProsecutioncaseEjected = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(spiProsecutioncaseEjected.getCaseId());
        final SPIPoliceCase spiPoliceCase = aggregateService.get(eventStream, SPIPoliceCase.class);
        final Stream<Object> newEvents = spiPoliceCase.handleEjectCase(spiProsecutioncaseEjected.getCaseId());
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }

    @Handles("stagingprosecutorsspi.command.handler.filter-prosecution-case")
    public void processFilterProsecutionCase(final Envelope<FilterProsecutionCase> envelope) throws EventStreamException {
        final FilterProsecutionCase filterProsecutionCase = envelope.payload();
        final String ptiUrn = filterProsecutionCase.getPtiUrn();
        final UUID caseId = systemMapperService.getCaseIdForPtiURNAndOriginatingOrganisation(ptiUrn);
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final SPIPoliceCase spiPoliceCase = aggregateService.get(eventStream, SPIPoliceCase.class);
        final Stream<Object> newEvents = spiPoliceCase.filterProsecutionCase(caseId);
        appendMetaDataInEventStream(envelope, eventStream, newEvents);
    }
    @Handles("stagingprosecutorsspi.command.handler.spi-oi-update-police-system-id")
    public void updatePoliceSystem(final Envelope<SpiOiUpdatePoliceSystemId> spiOiUpdatePoliceSystemIdEnvelope) throws EventStreamException {
        final SpiOiUpdatePoliceSystemId spiOiUpdatePoliceSystemId = spiOiUpdatePoliceSystemIdEnvelope.payload();
        final UUID oiToUpdate = spiOiUpdatePoliceSystemId.getId();
        final EventStream eventStream = eventSource.getStreamById(oiToUpdate);
        final OIMessage oiMessage = aggregateService.get(eventStream, OIMessage.class);
        final Stream<Object> newEvents = oiMessage.updatePoliceSystemId(oiToUpdate, spiOiUpdatePoliceSystemId.getPoliceSystemId());
        appendMetaDataInEventStream(spiOiUpdatePoliceSystemIdEnvelope, eventStream, newEvents);

    }
}

