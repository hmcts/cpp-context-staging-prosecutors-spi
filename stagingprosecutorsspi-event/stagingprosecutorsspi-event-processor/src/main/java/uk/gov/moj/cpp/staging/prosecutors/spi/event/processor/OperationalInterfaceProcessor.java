package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static javax.xml.bind.JAXBContext.newInstance;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.cjse.schemas.common.operations.ObjectFactory;
import uk.gov.cjse.schemas.common.operations.RouteDataResponseType;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MdiIdWithMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OperationalDetailsPreparedForResponse;

import java.io.StringWriter;

import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

@ServiceComponent(EVENT_PROCESSOR)
public class OperationalInterfaceProcessor {

    private static final String SPI_COMMAND_PREPARE_MDI_FOR_CPP_MESSAGE = "stagingprosecutorsspi.command.handler.prepare-cpp-message-for-sending";
    @Inject
    @Value(key = "cpp.system.id.spi.in", defaultValue = "B00LIBRA")
    private String cppSystemId;
    @Inject
    private Sender sender;

    @Handles("stagingprosecutorsspi.event.operationalDetails-prepared-for-response")
    public void processOperationalDetailsPreparedForResponse(final Envelope<OperationalDetailsPreparedForResponse> envelope) throws JAXBException {
        final OperationalDetailsPreparedForResponse operationalDetailsPreparedForResponse = envelope.payload();
        final String mdiMessage = getOIResponseAsString(operationalDetailsPreparedForResponse.getRouteDataResponseType());
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(SPI_COMMAND_PREPARE_MDI_FOR_CPP_MESSAGE)
                .build();
        final MdiIdWithMessage mdiIdWithMessage = new MdiIdWithMessage(operationalDetailsPreparedForResponse.getMessageId(), mdiMessage, cppSystemId);
        final Envelope<MdiIdWithMessage> mdiMessageEnvelope = envelopeFrom(metadata, mdiIdWithMessage);
        sender.send(mdiMessageEnvelope);
    }

    private String getOIResponseAsString(final RouteDataResponseType routeDataResponseType) throws JAXBException {
        final JAXBElement<RouteDataResponseType> routeDataResponseTypeJAXBElement = new ObjectFactory().createRouteDataResp(routeDataResponseType);
        final StringWriter stringWriter = new StringWriter();
        newInstance(RouteDataResponseType.class).createMarshaller().marshal(routeDataResponseTypeJAXBElement, stringWriter);
        return stringWriter.toString();
    }


}