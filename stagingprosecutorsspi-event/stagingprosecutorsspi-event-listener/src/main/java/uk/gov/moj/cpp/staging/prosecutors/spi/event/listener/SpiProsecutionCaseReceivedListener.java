package uk.gov.moj.cpp.staging.prosecutors.spi.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.CPPMessageRepository;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class SpiProsecutionCaseReceivedListener {

    @Inject
    private CPPMessageRepository cppMessageRepository;

    @Handles("stagingprosecutorsspi.event.prosecution-case-received")
    public void spiProsecutionCaseReceivedListener(final Envelope<SpiProsecutionCaseReceived> envelope) {
        final SpiProsecutionCaseReceived spiProsecutionCaseReceived = envelope.payload();
        final CPPMessage cppMessage = cppMessageRepository.findBy(envelope.payload().getOiId());
        cppMessage.setCaseId(spiProsecutionCaseReceived.getCaseId());
        cppMessage.setPtiUrn(spiProsecutionCaseReceived.getPoliceCase().getCaseDetails().getPtiurn());
        cppMessageRepository.save(cppMessage);

    }

}
