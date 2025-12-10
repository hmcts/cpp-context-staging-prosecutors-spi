package uk.gov.moj.cpp.staging.command.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.RouteDataResponseType;
import uk.gov.cjse.schemas.common.operations.SystemDetailsStructure;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.justice.staging.prosecutors.json.schemas.SpiProsecutioncaseEjected;
import uk.gov.moj.cpp.staging.command.service.ReferenceDataQueryService;
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
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.ResendMessage;
import uk.gov.moj.cpp.staging.soap.schema.OIDetails;

import java.util.ArrayList;
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
public class StagingProsecutorsSpiCommandHandlerTest {

    private static final String CJSE_RECEIVE_COMMAND_HANDLER_TOPIC = "hmcts.cjs.process-spi-prosecution-case";
    private static final String CJSE_RESEND_MESSAGE_TOPIC = "hmcts.cjs.resend-message";
    private static final String OI_RECEIVE_COMMAND_HANDLER_TOPIC = "stagingprosecutorsspi.command.handler.spi-oi-receive";
    private static final String OI_RECEIVE_RESPONSE_COMMAND_HANDLER_TOPIC = "stagingprosecutorsspi.command.handler.spi-oi-receive_response";
    private static final String PREPARE_CPP_MESSAGE_COMMAND_HANDLER_TOPIC = "stagingprosecutorsspi.command.handler.prepare-cpp-message-for-sending";
    private static final String PREPARE_ASYNC_RESPONSE_COMMAND_HANDLER_TOPIC = "stagingprosecutorsspi.command.handler.preparreceiveSpiOie-async-error-response";
    private static final String SPI_PROSECUTION_CASE_RECEIVE_COMMAND_HANDLER_TOPIC = "stagingprosecutorsspi.command.handler.receive-prosecution-case";
    private static final String STAGINGPROSECUTORSSPI_COMMAND_HANDLER_SPI_PROSECUTIONCASE_EJECTED = "stagingprosecutorsspi.command.handler.spi-prosecutioncase-ejected";
    private static final String STAGINGPROSECUTORSSPI_COMMAND_HANDLER_SPI_PROSECUTIONCASE_FILTERED = "stagingprosecutorsspi.command.handler.filter-prosecution-case";

    private static final String STAGINGPROSECUTORSSPI_COMMAND_HANDLER_UPDATE_POLICE_SYSTEM_ID= "stagingprosecutorsspi.command.handler.spi-oi-update-police-system-id";


    private static final UUID CJSE_MESSAGE_ID = randomUUID();
    private static final String POLICE_SYSTEM_ID = new StringGenerator().next();
    private static final String PTI_URN = new StringGenerator().next();

    private static final UUID CORRELATION_ID = randomUUID();

    @InjectMocks
    private StagingProsecutorsSpiCommandHandler stagingProsecutorsSpiCommandHandler;


    @Mock
    private SystemMapperService systemMapperService;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private EventStream eventStream;

    @Mock
    private CPPMessage cppMessage;
    @Mock
    private CJSEMessage cjseMessage;
    @Mock
    private SPIPoliceCase spiPoliceCase;
    @Mock
    private OIMessage oiMessage;

    @Mock
    private RouteDataRequestType routeDataRequestType;
    @Mock
    private RouteDataResponseType routeDataResponseType;
    @Mock
    private SystemDetailsStructure systemDetailsStructure;
    @Mock
    private SystemDetailsStructure.SystemID systemID;

    @Mock
    private Stream<Object> newEvents;
    @Mock
    private Stream<Object> mappedNewEvents;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptorStream;

    @Test
    public void shouldGenerateCJSERequestReceivedEvent() throws EventStreamException {
        final CjseMessage cjseMessageRequest = getMockCjseMessageRequest();
        final Envelope<CjseMessage> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(CJSE_RECEIVE_COMMAND_HANDLER_TOPIC),
                cjseMessageRequest
        );


        when(systemMapperService.getMappedUUIDForRequestId(cjseMessageRequest.getRequestId())).thenReturn(envelope.metadata().id());
        when(eventSource.getStreamById(envelope.metadata().id())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CJSEMessage.class)).thenReturn(cjseMessage);
        when(cjseMessage.receiveCjseMessage(cjseMessageRequest)).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingProsecutorsSpiCommandHandler.receiveSpiCjseMessage(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    private CjseMessage getMockCjseMessageRequest() {
        return CjseMessage.cjseMessage()
                .withRequestId("21_05_2018_103408_000000306578")
                .withSourceId("sourceId")
                .withExecMode("Async")
                .withDestinationID(new ArrayList<>())
                .withMessage("message")
                .withTimestamp("2016-09-08T16:11:24.436Z")
                .build();
    }

    @Test
    public void shouldGenerateCPPMessagePreparedForSendingEvent() throws EventStreamException {
        final ResendMessage resendMessageRequest = getMockResendMessageRequest();
        final Envelope<ResendMessage> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(CJSE_RESEND_MESSAGE_TOPIC),
                resendMessageRequest
        );

        when(eventSource.getStreamById(resendMessageRequest.getId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CPPMessage.class)).thenReturn(cppMessage);
        when(cppMessage.resendMessage(resendMessageRequest.getId())).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingProsecutorsSpiCommandHandler.resendCPPMessage(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    private ResendMessage getMockResendMessageRequest() {
        return ResendMessage.resendMessage()
                .withId(randomUUID())
                .build();
    }

    @Test
    public void shouldGenerateCPPMessagePreparedForSendingEventWhenPreparingCPPMessage() throws EventStreamException {
        final MdiIdWithMessage mdiIdWithMessageRequest = getMockMDIRequest();
        final Envelope<MdiIdWithMessage> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(PREPARE_CPP_MESSAGE_COMMAND_HANDLER_TOPIC),
                mdiIdWithMessageRequest
        );

        when(eventSource.getStreamById(mdiIdWithMessageRequest.getId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CPPMessage.class)).thenReturn(cppMessage);
        when(cppMessage.prepareCPPMessageForSending(eq(mdiIdWithMessageRequest.getId().toString()), eq(mdiIdWithMessageRequest.getMessage()), eq(mdiIdWithMessageRequest.getSystemId()), any())).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingProsecutorsSpiCommandHandler.prepareCPPMessage(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    private MdiIdWithMessage getMockMDIRequest() {
        return new MdiIdWithMessage(CJSE_MESSAGE_ID, "Message", "C00CommonPlatform");
    }

    @Test
    public void shouldGenerateOIRequestReceivedEvent() throws EventStreamException {
        reset(systemMapperService);
        final OIDetails oiDetailsRequest = getMockOIDetailsRequest();
        final Envelope<OIDetails> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(OI_RECEIVE_COMMAND_HANDLER_TOPIC),
                oiDetailsRequest
        );
        final UUID oiID = randomUUID();

        when(routeDataRequestType.getRequestFromSystem()).thenReturn(systemDetailsStructure);
        when(systemDetailsStructure.getCorrelationID()).thenReturn(CORRELATION_ID.toString());
        when(systemDetailsStructure.getSystemID()).thenReturn(systemID);
        when(systemID.getValue()).thenReturn("systemID");
        when(systemMapperService.getOiIdForCorrelationAndSystemId(CORRELATION_ID.toString(),"systemID")).thenReturn(oiID);
        when(eventSource.getStreamById(oiID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, OIMessage.class)).thenReturn(oiMessage);
        when(oiMessage.oiRequestReceived(any(OIDetails.class), any(UUID.class), any())).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingProsecutorsSpiCommandHandler.receiveSpiOiRequestMessage(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    private OIDetails getMockOIDetailsRequest() {
        return new OIDetails(CJSE_MESSAGE_ID, routeDataRequestType);
    }

    @Test
    public void shouldGenerateOIErrorEvent() throws EventStreamException {
        reset(systemMapperService);
        final OIDetails oiDetailsRequest = getMockOIDetailsResponse();
        final Envelope<OIDetails> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(OI_RECEIVE_RESPONSE_COMMAND_HANDLER_TOPIC),
                oiDetailsRequest
        );
        final UUID oiID = randomUUID();

        when(routeDataResponseType.getResponseToSystem()).thenReturn(systemDetailsStructure);
        when(systemDetailsStructure.getCorrelationID()).thenReturn(CORRELATION_ID.toString());
        when(systemDetailsStructure.getSystemID()).thenReturn(systemID);
        when(systemID.getValue()).thenReturn("systemID");
        final String correlationAndSystemId = CORRELATION_ID.toString().concat("systemID");
        when(eventSource.getStreamById(CORRELATION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, OIMessage.class)).thenReturn(oiMessage);
        when(oiMessage.oiResponseReceived(any(OIDetails.class))).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingProsecutorsSpiCommandHandler.receiveOiResponseMessage(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    private OIDetails getMockOIDetailsResponse() {
        return new OIDetails(CJSE_MESSAGE_ID, routeDataResponseType);
    }

    @Test
    public void shouldGenerateSPIRequestReceivedEvent() throws EventStreamException {
        final PoliceCase policeCaseRequest = getMockPoliceCase();
        final UUID oiId = randomUUID();
        final ReceiveProsecutionCase receiveProsecutionCase = ReceiveProsecutionCase.receiveProsecutionCase()
                .withPoliceCase(policeCaseRequest)
                .withOiId(oiId)
                .build();
        final Envelope<ReceiveProsecutionCase> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(SPI_PROSECUTION_CASE_RECEIVE_COMMAND_HANDLER_TOPIC),
                receiveProsecutionCase
        );

        final UUID caseId = randomUUID();

        when(systemMapperService.getCaseIdForPtiURNAndOriginatingOrganisation(policeCaseRequest.getCaseDetails().getPtiurn())).thenReturn(caseId);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SPIPoliceCase.class)).thenReturn(spiPoliceCase);
        when(spiPoliceCase.receivePoliceCase(caseId, oiId, policeCaseRequest)).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingProsecutorsSpiCommandHandler.processProsecutionCase(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    @Test
    public void shouldHandleEjectPoliceCase() throws EventStreamException {

        final UUID caseId = randomUUID();

        final SpiProsecutioncaseEjected spiProsecutioncaseEjected = SpiProsecutioncaseEjected.spiProsecutioncaseEjected()
                .withCaseId(caseId)
                .build();

        final Envelope<SpiProsecutioncaseEjected> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(STAGINGPROSECUTORSSPI_COMMAND_HANDLER_SPI_PROSECUTIONCASE_EJECTED),
                spiProsecutioncaseEjected);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SPIPoliceCase.class)).thenReturn(spiPoliceCase);
        when(spiPoliceCase.handleEjectCase(caseId)).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingProsecutorsSpiCommandHandler.handleEjectedSPICase(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    @Test
    public void shouldHandleFilterPoliceCase() throws EventStreamException {

        final UUID caseId = randomUUID();
        final String ptiUrn = "URN";
        final FilterProsecutionCase spiProsecutioncaseFiltered = FilterProsecutionCase.filterProsecutionCase()
                .withPtiUrn(ptiUrn)
                .build();

        final Envelope<FilterProsecutionCase> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(STAGINGPROSECUTORSSPI_COMMAND_HANDLER_SPI_PROSECUTIONCASE_FILTERED),
                spiProsecutioncaseFiltered);
        when(systemMapperService.getCaseIdForPtiURNAndOriginatingOrganisation(ptiUrn)).thenReturn(caseId);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, SPIPoliceCase.class)).thenReturn(spiPoliceCase);
        when(spiPoliceCase.filterProsecutionCase(caseId)).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingProsecutorsSpiCommandHandler.processFilterProsecutionCase(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    @Test
    public void shouldHandleUpdatePoliceSystem() throws EventStreamException {

        final UUID oiId = randomUUID();
        final String policeSystemId = "policeSystemId";
        final SpiOiUpdatePoliceSystemId spiOiUpdatePoliceSystemId = new SpiOiUpdatePoliceSystemId(oiId, policeSystemId);

        final Envelope<SpiOiUpdatePoliceSystemId> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(STAGINGPROSECUTORSSPI_COMMAND_HANDLER_UPDATE_POLICE_SYSTEM_ID),
                spiOiUpdatePoliceSystemId);
        when(eventSource.getStreamById(oiId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, OIMessage.class)).thenReturn(oiMessage);
        when(oiMessage.updatePoliceSystemId(oiId, policeSystemId)).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingProsecutorsSpiCommandHandler.updatePoliceSystem(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    private PoliceCase getMockPoliceCase() {
        return PoliceCase.policeCase().withCaseDetails(
                CaseDetails.caseDetails()
                        .withPtiurn("URN")
                        .build())
                .build();
    }

    @Test
    public void shouldGenerateOperationalDetailsPreparedForResponseEvent() throws EventStreamException {
        final PrepareAsyncErrorResponse prepareAsyncErrorResponse = getMockPrepareAsyncErrorResponse();
        final Envelope<PrepareAsyncErrorResponse> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(PREPARE_ASYNC_RESPONSE_COMMAND_HANDLER_TOPIC),
                prepareAsyncErrorResponse
        );

        when(eventSource.getStreamById(prepareAsyncErrorResponse.getOiId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, OIMessage.class)).thenReturn(oiMessage);
        when(oiMessage.prepareOIResponseForXSDFailures(prepareAsyncErrorResponse.getErrorMessage())).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        stagingProsecutorsSpiCommandHandler.prepareAsyncErrorResponse(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    private PrepareAsyncErrorResponse getMockPrepareAsyncErrorResponse() {
        return new PrepareAsyncErrorResponse("Error", CJSE_MESSAGE_ID, POLICE_SYSTEM_ID, PTI_URN);
    }

    private void checkEventsAppendedAreMappedNewEvents() throws EventStreamException {
        verify(eventStream).append(argumentCaptorStream.capture());
        final Stream<JsonEnvelope> stream = argumentCaptorStream.getValue();
        assertThat(stream, is(mappedNewEvents));
    }

}
