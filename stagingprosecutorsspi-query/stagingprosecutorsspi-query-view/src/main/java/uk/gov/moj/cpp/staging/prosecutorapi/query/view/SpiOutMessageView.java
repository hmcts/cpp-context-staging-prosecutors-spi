package uk.gov.moj.cpp.staging.prosecutorapi.query.view;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SpiOutMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.SpiOutMessageRepository;

import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(QUERY_VIEW)
public class SpiOutMessageView {

    @Inject
    private Enveloper enveloper;

    @Inject
    private SpiOutMessageRepository spiOutMessageRepository;

    @Handles("stagingprosecutorsspi.query.spi-out-message-view")
    public JsonEnvelope querySpiOutMessage(final JsonEnvelope envelope) {
        final String caseUrn = envelope.payloadAsJsonObject().getString("caseUrn");
        final String defendantProsecutorReference = envelope.payloadAsJsonObject().getString("defendantProsecutorReference");

        final List<SpiOutMessage> latestSpiOutMessages = spiOutMessageRepository.findLatestSpiMessageForCaseUrnAndDefendantReference(caseUrn, defendantProsecutorReference);

        if (isEmpty(latestSpiOutMessages)) {
            return envelopeFrom(envelope.metadata(), (JsonObject) null);
        }

        return envelopeFrom(JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("stagingprosecutorsspi.query.spiout.message"), getResponse(caseUrn, defendantProsecutorReference, latestSpiOutMessages.get(0)));
    }

    private JsonObject getResponse(final String caseUrn, final String defendantProsecutorReference, final SpiOutMessage spiOutMessage) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add("caseUrn", caseUrn)
                .add("defendantProsecutorReference", defendantProsecutorReference)
                .add("payload", spiOutMessage.getPayload());

        return builder.build();
    }
}
