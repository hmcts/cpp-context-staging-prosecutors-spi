package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionCaseFilteredProcessor {

    private static final String PUBLIC_CASE_FILTERED = "public.stagingprosecutorsspi.event.prosecution-case-filtered";

    @Inject
    private Sender sender;

    @Handles("stagingprosecutorsspi.event.prosecution-case-filtered")
    public void handleProsecutionCaseFiltered(final JsonEnvelope envelope) {
        sender.send(
                envelop(envelope.payloadAsJsonObject())
                        .withName(PUBLIC_CASE_FILTERED)
                        .withMetadataFrom(envelope));
    }

}
