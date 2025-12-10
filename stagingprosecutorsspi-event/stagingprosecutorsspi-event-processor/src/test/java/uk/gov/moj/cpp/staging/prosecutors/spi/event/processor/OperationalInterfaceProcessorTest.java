package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;


import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.ErrorDetails.errorDetails;

import uk.gov.cjse.schemas.common.operations.RouteDataResponseType;
import uk.gov.cjse.schemas.common.operations.SystemDetailsStructure;
import uk.gov.cjse.schemas.endpoint.types.ExecMode;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MdiIdWithMessage;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSyncResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OperationalDetailsPreparedForResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiError;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.ErrorDetails;

import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OperationalInterfaceProcessorTest {

    private static final String SPI_COMMAND_PREPARE_MDI_FOR_CPP_MESSAGE = "stagingprosecutorsspi.command.handler.prepare-cpp-message-for-sending";
    private static final String PREPARED_FOR_RESPONSE_EVENT = "stagingprosecutorsspi.event.operationalDetails-prepared-for-response";


    @InjectMocks
    private  OperationalInterfaceProcessor operationalInterfaceProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<MdiIdWithMessage>> envelopeArgumentCaptor;


    @Test
    public void processOperationalDetailsPreparedForResponseTest() throws JAXBException {
        OperationalDetailsPreparedForResponse operationalDetailsPreparedForResponse = getMockOperationalDetailsPreparedForResponse();
        final Envelope<OperationalDetailsPreparedForResponse> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(PREPARED_FOR_RESPONSE_EVENT),operationalDetailsPreparedForResponse);
        operationalInterfaceProcessor.processOperationalDetailsPreparedForResponse(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<MdiIdWithMessage> captorValue = envelopeArgumentCaptor.getValue();

        assertThat(captorValue.payload(), is(notNullValue()));
        assertThat(captorValue.metadata().name(), is(SPI_COMMAND_PREPARE_MDI_FOR_CPP_MESSAGE));
    }

    private OperationalDetailsPreparedForResponse getMockOperationalDetailsPreparedForResponse() {
        CjseMessage.cjseMessage().withExecMode(ExecMode.SYNCH.value()).withDestinationID(asList(UUID.randomUUID().toString()));
        final RouteDataResponseType routeDataResponseType = new RouteDataResponseType();
        final String routeId = UUID.randomUUID().toString();
        routeDataResponseType.setResponseToSystem(new SystemDetailsStructure());
        final RouteDataResponseType.OperationStatus operationStatus = new RouteDataResponseType.OperationStatus();
        operationStatus.setCode(1310);
        operationStatus.setStatusClass("FatalError");
        operationStatus.setResponseContext("error test");
        operationStatus.setDescription("UnusableDataStream");
        operationStatus.setRouteId(routeId);
        routeDataResponseType.getOperationStatus().add(operationStatus);
        return new OperationalDetailsPreparedForResponse(UUID.randomUUID(), routeDataResponseType);
    }
}