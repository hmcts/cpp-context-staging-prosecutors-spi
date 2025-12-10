package uk.gov.moj.cpp.staging.command.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.command.service.SystemMapperService;
import uk.gov.moj.cpp.staging.prosecutors.spi.aggregate.OIMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.SendSpiResult;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiResult;

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
public class StagingResultHandlerTest {
    private static final String SEND_SPI_RESULT_COMMAND_HANDLER_TOPIC = "stagingprosecutorsspi.command.handler.send-spi-result";

    @InjectMocks
    private StagingResultHandler stagingResultHandler;

    @Mock
    private OIMessage oIMessage;
    @Mock
    private Stream<Object> newEvents;
    @Mock
    private EventStream eventStream;

    @Mock
    private SystemMapperService systemMapperService;
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private Stream<Object> mappedNewEvents;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptorStream;

    @Test
    public void shouldsendSpiResult() throws EventStreamException {
        final String spiResultId = "21_05_2018_103408_000000306578";
        final SendSpiResult sendSpiResult = getMockSendSpiResult(spiResultId);
        final Envelope<SendSpiResult> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(SEND_SPI_RESULT_COMMAND_HANDLER_TOPIC),
                sendSpiResult
        );
        when(systemMapperService.getSpiResultIdForURN(sendSpiResult.getSpiResult().getPtiUrn())).thenReturn(envelope.metadata().id());
        final UUID oiId = randomUUID();
        when(systemMapperService.getOiIdForCorrelationAndSystemId(any(), any())).thenReturn(oiId);
        when(eventSource.getStreamById(oiId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, OIMessage.class)).thenReturn(oIMessage);
        when(oIMessage.prepareSPIResult(eq(envelope.metadata().id()), eq(sendSpiResult.getSpiResult()), eq(null), any(UUID.class))).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);
        stagingResultHandler.handleSendSpiResultCommand(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    private SendSpiResult getMockSendSpiResult(final String spiResultId) {
        return SendSpiResult.sendSpiResult()
                .withSpiResult(SpiResult.spiResult()
                        .withPtiUrn(spiResultId)
                        .withPayload("payload")
                        .withDataController("dataController")
                        .withDestinationSystemId(spiResultId)
                        .withOrganizationalUnitID(spiResultId)
                        .build())
                .build();
    }

    private void checkEventsAppendedAreMappedNewEvents() throws EventStreamException {
        verify(eventStream).append(argumentCaptorStream.capture());
        final Stream<JsonEnvelope> stream = argumentCaptorStream.getValue();
        assertThat(stream, is(mappedNewEvents));
    }
}
