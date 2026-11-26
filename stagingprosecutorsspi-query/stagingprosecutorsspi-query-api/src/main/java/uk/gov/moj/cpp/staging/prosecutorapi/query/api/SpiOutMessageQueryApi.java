package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(QUERY_API)
public class SpiOutMessageQueryApi {

    @Inject
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Handles("stagingprosecutorsspi.query.spi-out-message")
    public JsonEnvelope querySpiMessage(final JsonEnvelope envelope) {
        final JsonEnvelope queryEnvelop = envelopeFrom(JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("stagingprosecutorsspi.query.spi-out-message-view"), envelope.payloadAsJsonObject());

        return requester.request(queryEnvelop);
    }
}
