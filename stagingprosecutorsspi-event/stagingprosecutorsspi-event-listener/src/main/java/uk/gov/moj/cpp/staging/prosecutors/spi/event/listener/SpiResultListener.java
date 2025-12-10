package uk.gov.moj.cpp.staging.prosecutors.spi.event.listener;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SpiOutMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.SpiOutMessageRepository;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.SpiResultPreparedForSending;

import java.time.ZonedDateTime;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class SpiResultListener {

    private static final String PROSECUTOR_REFERENCE_ATTRIBUTE = "ProsecutorReference";

    @Inject
    private SpiOutMessageRepository spiOutMessageRepository;

    @Handles("stagingprosecutorsspi.event.spi-result-prepared-for-sending")
    public void onSpiResultPreparedForSending(final Envelope<SpiResultPreparedForSending> envelope) {
        final SpiResultPreparedForSending spiResultPreparedForSending = envelope.payload();
        final String defendantReference = getDefendantReference(spiResultPreparedForSending);
        final ZonedDateTime timestamp = envelope.metadata().createdAt().orElse(ZonedDateTime.now());
        final String caseUrn = spiResultPreparedForSending.getPtiUrn();
        spiOutMessageRepository.save(new SpiOutMessage(randomUUID(), caseUrn, timestamp, defendantReference, spiResultPreparedForSending.getPayload()));
    }

    private String getDefendantReference(final SpiResultPreparedForSending spiResultPreparedForSending) {
        return substringBetween(spiResultPreparedForSending.getPayload(), "<" + PROSECUTOR_REFERENCE_ATTRIBUTE + ">", "</" + PROSECUTOR_REFERENCE_ATTRIBUTE + ">");
    }
}
