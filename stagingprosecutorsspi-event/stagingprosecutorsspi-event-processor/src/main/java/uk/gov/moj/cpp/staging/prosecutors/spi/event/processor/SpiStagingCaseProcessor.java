package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.RouteDataResponseType;
import uk.gov.dca.xmlschemas.libra.StdProsPoliceNewCaseStructure;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.ReceiveProsecutionCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PoliceCaseHelper;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OiRequestMessageReceived;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseRequestMessageReceived;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;
import uk.gov.moj.cpp.staging.soap.schema.OIDetails;
import uk.gov.moj.cpp.staging.soap.schema.ObjectUnMarshaller;
import uk.gov.moj.cpp.staging.soap.schema.converter.SpiCaseConverter;

import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

@ServiceComponent(EVENT_PROCESSOR)
public class SpiStagingCaseProcessor {
    static final String SPI_COMMAND_RECEIVE_CASE_COMMAND = "stagingprosecutorsspi.command.handler.receive-prosecution-case";
    static final String OI_REQUEST_RECEIVE_COMMAND = "stagingprosecutorsspi.command.handler.spi-oi-receive";
    static final String OI_RECEIVE_RESPONSE_COMMAND = "stagingprosecutorsspi.command.handler.spi-oi-receive_response";

    @Inject
    private Sender sender;

    @Handles("stagingprosecutorsspi.event.cjse-request-message-received")
    @SuppressWarnings("squid:S1160")
    public void onCjseRequestMessageReceived(final Envelope<CjseRequestMessageReceived> cjseRequestMessageReceivedEnvelope) throws JAXBException {
        final Object requestOrResponseType = ((JAXBElement) new ObjectUnMarshaller().getRequestOrResponseType(cjseRequestMessageReceivedEnvelope.payload().getCjseMessage().getMessage())).getValue();
        final Envelope<OIDetails> envelope = requestOrResponseType instanceof RouteDataRequestType ? getOiDetailsEnvelopeForCJSERequest(cjseRequestMessageReceivedEnvelope, (RouteDataRequestType) requestOrResponseType) :
                getOiDetailsEnvelopeForResponse(cjseRequestMessageReceivedEnvelope, (RouteDataResponseType) requestOrResponseType);
        sender.send(envelope);
    }

    private Envelope<OIDetails> getOiDetailsEnvelopeForCJSERequest(Envelope<CjseRequestMessageReceived> cjseRequestMessageReceivedEnvelope, RouteDataRequestType routeDataRequestType) {
        final OIDetails oiDetailsDataType = new OIDetails(randomUUID(), routeDataRequestType);
        final Metadata metadata = metadataFrom(cjseRequestMessageReceivedEnvelope.metadata())
                .withName(OI_REQUEST_RECEIVE_COMMAND)
                .build();
        return envelopeFrom(metadata, oiDetailsDataType);
    }

    private Envelope<OIDetails> getOiDetailsEnvelopeForResponse(Envelope<CjseRequestMessageReceived> cjseRequestMessageReceivedEnvelope, RouteDataResponseType routeDataResponseType) {
        final OIDetails oiDetailsDataType = new OIDetails(randomUUID(), routeDataResponseType);
        final Metadata metadata = metadataFrom(cjseRequestMessageReceivedEnvelope.metadata())
                .withName(OI_RECEIVE_RESPONSE_COMMAND)
                .build();
        return envelopeFrom(metadata, oiDetailsDataType);
    }

    @Handles("stagingprosecutorsspi.event.spi-oi-request-received")
    @SuppressWarnings("squid:S1160")
    public void onOiRequestReceived(final Envelope<OiRequestMessageReceived> oiRequestMessageReceivedEnvelope) throws JAXBException, SAXException {
        final StdProsPoliceNewCaseStructure stdProsPoliceNewCaseStructure;
        final OiRequestMessageReceived oiRequestMessageReceived = oiRequestMessageReceivedEnvelope.payload();

        stdProsPoliceNewCaseStructure = new ObjectUnMarshaller().getStdProsPoliceNewCaseStructure(oiRequestMessageReceived.getDataStreamContent());
        final PoliceCase policeCase = new SpiCaseConverter().convert(stdProsPoliceNewCaseStructure.getCase());

        final ReceiveProsecutionCase receiveProsecutionCase = ReceiveProsecutionCase.receiveProsecutionCase()
                .withOiId(oiRequestMessageReceived.getOiId())
                .withPoliceCase(PoliceCaseHelper.getPoliceCaseWithSystemIdAndOrganisation(policeCase, oiRequestMessageReceived.getPoliceSystemId()))
                .build();
        final Metadata metadata = metadataFrom(oiRequestMessageReceivedEnvelope.metadata())
                .withName(SPI_COMMAND_RECEIVE_CASE_COMMAND)
                .build();

        sender.send(envelopeFrom(metadata, receiveProsecutionCase));
    }
}
