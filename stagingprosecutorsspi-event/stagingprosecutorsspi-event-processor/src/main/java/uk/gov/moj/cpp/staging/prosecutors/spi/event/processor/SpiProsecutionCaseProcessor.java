package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.PrepareAsyncErrorResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.SpiToPCFConverter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicProsecutionCaseUnsupported;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
public class SpiProsecutionCaseProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpiProsecutionCaseProcessor.class);

    public static final String PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION = "prosecutioncasefile.command.initiate-sjp-prosecution";
    public static final String PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION = "prosecutioncasefile.command.initiate-cc-prosecution";
    public static final String SJP_CASE_INITIATION_CODE = "J";
    static final String PREPARE_ASYNC_ERROR_RESPONSE_COMMAND = "stagingprosecutorsspi.command.handler.prepare-async-error-response";

    @Inject
    private Sender sender;

    @Inject
    private Clock clock;

    @Inject
    private SpiToPCFConverter spiToPCFConverter;


    @Handles("stagingprosecutorsspi.event.prosecution-case-received")
    public void onSpiProsecutionCaseReceived(final Envelope<SpiProsecutionCaseReceived> envelope) {
        final SpiProsecutionCaseReceived spiProsecutionCaseReceived = envelope.payload();
        final ZonedDateTime dateReceived = envelope.metadata().createdAt().orElse(clock.now());
        final InitiateProsecution initiateProsecution = spiToPCFConverter.convert(spiProsecutionCaseReceived, dateReceived);
        final String metaDataName = isSjpCase(spiProsecutionCaseReceived) ? PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION : PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION;

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(metaDataName)
                .build();

        final JsonObject payload = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper()).convert(initiateProsecution);
        sender.sendAsAdmin(envelopeFrom(metadata, payload));
    }

    @Handles("public.prosecutioncasefile.prosecution-case-unsupported")
    public void onCaseUnsupportedMessageReceived(final Envelope<PublicProsecutionCaseUnsupported> prosecutionCaseUnsupported) {
        final PublicProsecutionCaseUnsupported payload = prosecutionCaseUnsupported.payload();
        if (payload.getChannel() == SPI) {
            final Metadata metadata = metadataFrom(prosecutionCaseUnsupported.metadata())
                    .withName(PREPARE_ASYNC_ERROR_RESPONSE_COMMAND)
                    .build();
            final PrepareAsyncErrorResponse prepareAsyncErrorResponse = PrepareAsyncErrorResponse.prepareAsyncErrorResponse()
                    .withErrorMessage(prosecutionCaseUnsupported.payload().getErrorMessage())
                    .withOiId(prosecutionCaseUnsupported.payload().getExternalId())
                    .withPoliceSystemId(prosecutionCaseUnsupported.payload().getPoliceSystemId())
                    .withPtiUrn(prosecutionCaseUnsupported.payload().getUrn())
                    .build();

            final Envelope envelope = envelopeFrom(metadata, prepareAsyncErrorResponse);

            sender.send(envelope);
        } else {
            LOGGER.info("Not interested in processing message for case with URN '{}' as its not related to SPI channel", payload.getUrn());
        }

    }

    private boolean isSjpCase(final SpiProsecutionCaseReceived spiProsecutionCaseReceived) {
        return SJP_CASE_INITIATION_CODE.equalsIgnoreCase(spiProsecutionCaseReceived.getPoliceCase().getCaseDetails().getCaseInitiationCode());
    }

}
