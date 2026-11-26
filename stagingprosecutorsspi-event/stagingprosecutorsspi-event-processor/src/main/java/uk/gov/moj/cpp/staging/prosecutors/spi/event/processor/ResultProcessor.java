package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static javax.xml.bind.JAXBContext.newInstance;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.SendSpiResult.sendSpiResult;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiResult.spiResult;

import uk.gov.cjse.schemas.common.operations.ObjectFactory;
import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.dca.xmlschemas.libra.StdProsPoliceResultedCaseStructure;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MdiIdWithMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.SendSpiResult;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.ResultConverter;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PublicPoliceResultGenerated;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.service.ProsecutionCaseFileService;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.SpiResultPreparedForSending;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiResult.Builder;

import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ResultProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultProcessor.class);

    private static final String SEND_SPI_RESULT_COMMAND = "stagingprosecutorsspi.command.handler.send-spi-result";
    private static final String SPI_COMMAND_PREPARE_MDI_FOR_CPP_MESSAGE = "stagingprosecutorsspi.command.handler.prepare-cpp-message-for-sending";
    private static final String STAGINGPROSECUTORSSPI_QUERY_CPP_MESSAGE = "stagingprosecutorsspi.query.cpp-message";
    private static final String POLICE_SYSTEM_ID = "policeSystemId";
    private static final String ORGANISATION_UNIT_ID = "organizationUnitID";
    private static final String DATA_CONTROLLER = "dataController";
    private static final String URN = "urn";
    private static final JAXBContext context;
    private static final String NON_POLICE_SYSTEM_ID = "00001NPPforB7";

    static {
        try {
            context = newInstance(StdProsPoliceResultedCaseStructure.class);
        } catch (JAXBException e) {
            LOGGER.error("Result Processor JAXB context for Police resulted case structure not successful", e);
            throw new IllegalStateException("Result Processor JAXB context for Police resulted case structure not successful");
        }
    }

    @Inject
    @Value(key = "cpp.system.id", defaultValue = "C00CommonPlatform")
    private String cppSystemId;

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Inject
    private ResultConverter resultConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Handles("public.results.police-result-generated")
    public void onSpiCaseGenerated(final JsonEnvelope envelope) throws JAXBException {

        final PublicPoliceResultGenerated publicPoliceResultGenerated = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PublicPoliceResultGenerated.class);
        final Map<String, Object> contextDetails = prosecutionCaseFileService.extractOffenceLocation(envelope, requester, publicPoliceResultGenerated.getCaseId());
        final StdProsPoliceResultedCaseStructure stdProsPoliceResultedCaseStructure = resultConverter.convert(publicPoliceResultGenerated, contextDetails);
        final String ptiUrn = envelope.payloadAsJsonObject().getString(URN);
        final Optional<JsonObject> cppMessageView = getCppMessage(ptiUrn, envelope.metadata());

        final Builder builder = spiResult()
                .withPayload(getStdProsPoliceResultedCaseStructure(stdProsPoliceResultedCaseStructure))
                .withPtiUrn(ptiUrn);

        if (cppMessageView.isPresent()) {
            final JsonObject jsonObject = cppMessageView.get();
            if (jsonObject.containsKey(POLICE_SYSTEM_ID)) {
                builder.withDestinationSystemId(jsonObject.getString(POLICE_SYSTEM_ID));
            } else {
                builder.withDestinationSystemId(NON_POLICE_SYSTEM_ID);
            }

            if (jsonObject.containsKey(ORGANISATION_UNIT_ID)) {
                builder.withOrganizationalUnitID(jsonObject.getString(ORGANISATION_UNIT_ID));
            }
            if (jsonObject.containsKey(DATA_CONTROLLER)) {
                builder.withDataController(jsonObject.getString(DATA_CONTROLLER));
            }
        } else {
            builder.withDestinationSystemId(NON_POLICE_SYSTEM_ID);
        }

        final SendSpiResult sendSpiResult = sendSpiResult()
                .withSpiResult(builder.build())
                .build();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(SEND_SPI_RESULT_COMMAND)
                .build();

        sender.send(envelopeFrom(metadata, sendSpiResult));
    }


    @Handles("stagingprosecutorsspi.event.spi-result-prepared-for-sending")
    public void onSpiResultPreparedForSending(final Envelope<SpiResultPreparedForSending> envelope) throws JAXBException {
        final SpiResultPreparedForSending spiResultPreparedForSending = envelope.payload();
        final String mdiMessage = getResultPayloadAsString(spiResultPreparedForSending);
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(SPI_COMMAND_PREPARE_MDI_FOR_CPP_MESSAGE)
                .build();
        final MdiIdWithMessage mdiIdWithMessage = new MdiIdWithMessage(spiResultPreparedForSending.getMessageId(), mdiMessage, cppSystemId);
        final Envelope<MdiIdWithMessage> mdiMessageEnvelope = envelopeFrom(metadata, mdiIdWithMessage);
        sender.send(mdiMessageEnvelope);

    }

    private String getStdProsPoliceResultedCaseStructure(final StdProsPoliceResultedCaseStructure stdProsPoliceResultedCaseStructure) throws JAXBException {
        final JAXBElement<StdProsPoliceResultedCaseStructure> routeDataRequestTypeJAXBElement = new uk.gov.dca.xmlschemas.libra.ObjectFactory().createResultedCaseMessage(stdProsPoliceResultedCaseStructure);

        final StringWriter stringWriter = new StringWriter();
        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        marshaller.marshal(routeDataRequestTypeJAXBElement, stringWriter);
        return stringWriter.toString();
    }

    private String getResultPayloadAsString(final SpiResultPreparedForSending spiResultPreparedForSending) throws JAXBException {
        final JAXBElement<RouteDataRequestType> routeDataRequestTypeJAXBElement = new ObjectFactory().createRouteData(spiResultPreparedForSending.getRouteDataRequestType());

        final StringWriter stringWriter = new StringWriter();
        newInstance(RouteDataRequestType.class).createMarshaller().marshal(routeDataRequestTypeJAXBElement, stringWriter);
        return stringWriter.toString();
    }

    private Optional<JsonObject> getCppMessage(final String ptiUrn, final Metadata metadata) {

        final Metadata queryMetadata = metadataFrom(metadata)
                .withName(STAGINGPROSECUTORSSPI_QUERY_CPP_MESSAGE)
                .build();

        final JsonObject ptiUrnJsonObject = createObjectBuilder().add("ptiUrn", ptiUrn).build();
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(queryMetadata, ptiUrnJsonObject);
        final Envelope<JsonObject> responseEnvelope = requester.requestAsAdmin(envelope, JsonObject.class);

        if (nonNull(responseEnvelope)) {
            final JsonObject payload = responseEnvelope.payload();

            if (nonNull(payload) && payload.containsKey("cppMessages")) {
                final JsonArray cppMessages = responseEnvelope.payload().getJsonArray("cppMessages");

                if (!cppMessages.isEmpty()) {
                    return ofNullable((JsonObject) cppMessages.get(0));
                }
            }
            return empty();
        }

        return empty();
    }

}
