package uk.gov.moj.cpp.staging.command.api;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.staging.command.api.converter.TimeOfHearingConverter.updateBSTTimeOfHearing;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(COMMAND_API)
public class StagingProsecutorsSpiCommandApi {

    private static final String MESSAGE_FIELD_KEY = "message";

    @Inject
    private Sender sender;


    @Handles("hmcts.cjs.receive-spi-message")
    public void receiveCJSEMessage(final JsonEnvelope envelope) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder();
        envelope.payloadAsJsonObject().entrySet().stream()
                .filter(stringJsonValueEntry -> !MESSAGE_FIELD_KEY.equals(stringJsonValueEntry.getKey()))
                .forEach(entry -> objectBuilder.add(entry.getKey(), entry.getValue()));
        sender.send(envelopeFrom(envelope.metadata(),
                objectBuilder.add(MESSAGE_FIELD_KEY,
                        updateBSTTimeOfHearing(envelope.payloadAsJsonObject().getString(MESSAGE_FIELD_KEY))).build()));
    }

    @Handles("hmcts.cjs.resend-message")
    public void resendCPPMessage(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("stagingprosecutorsspi.command.spi.receive-prosecution-case")
    public void spiReceivePoliceCase(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("stagingprosecutorsspi.command.handler.receive-prosecution-case"), envelope.payloadAsJsonObject()));
    }

    @Handles("stagingprosecutorsspi.command.spi.filter-prosecution-case")
    public void spiFilterProsecutionCase(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("stagingprosecutorsspi.command.handler.filter-prosecution-case"), envelope.payloadAsJsonObject()));
    }

    @Handles("stagingprosecutorsspi.command.spi-oi-update-police-system-id")
    public void updatePoliceSystem(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("stagingprosecutorsspi.command.handler.spi-oi-update-police-system-id"), envelope.payloadAsJsonObject()));
    }

}

