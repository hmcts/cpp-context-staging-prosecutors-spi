package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

@ServiceComponent(EVENT_PROCESSOR)
public class SPIPoliceCaseEjectedProcessor {

    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String CASE_ID = "caseId";
    private static final String STAGINGPROSECUTORSSPI_COMMAND_HANDLER_SPI_PROSECUTIONCASE_EJECTED = "stagingprosecutorsspi.command.handler.spi-prosecutioncase-ejected";
    private static final Logger LOGGER = LoggerFactory.getLogger(SPIPoliceCaseEjectedProcessor.class);


    @Inject
    private Sender sender;

    @Handles("public.progression.events.case-or-application-ejected")
    public void handleCaseOrApplicationEjected(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.containsKey(PROSECUTION_CASE_ID)) {
            final String caseId = payload.getString(PROSECUTION_CASE_ID);
            final JsonObject caseEjectedCommandPayload = JsonObjects.createObjectBuilder()
                    .add(CASE_ID, caseId)
                    .build();
            final Metadata metadata = metadataFrom(envelope.metadata())
                    .withName(STAGINGPROSECUTORSSPI_COMMAND_HANDLER_SPI_PROSECUTIONCASE_EJECTED)
                    .build();
            sender.sendAsAdmin(JsonEnvelope.envelopeFrom(metadata, caseEjectedCommandPayload));

        } else  {
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("The Payload has been ignored as it does not contains caseId : {}" ,  envelope.toObfuscatedDebugString());
            }
        }
    }

}
