package uk.gov.moj.cpp.staging.prosecutors.spi.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.CPPMessageRepository;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.listener.SpiProsecutionCaseReceivedListener;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SpiProsecutionCaseReceivedListenerTest {
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OI_ID = UUID.randomUUID();

    @Mock
    private Envelope<SpiProsecutionCaseReceived> spiProsecutionCaseReceivedEnvelope;

    @Mock
    private CPPMessageRepository cppMessageRepository;


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SpiProsecutionCaseReceived spiProsecutionCaseReceived;

    @InjectMocks
    private SpiProsecutionCaseReceivedListener spiProsecutionCaseRecivedListener;

    @Test
    public void spiProsecutionCaseReceived() {
        when(spiProsecutionCaseReceivedEnvelope.payload()).thenReturn(spiProsecutionCaseReceived);
        when(spiProsecutionCaseReceived.getPoliceCase().getCaseDetails().getPtiurn()).thenReturn("pti urn");
        when(spiProsecutionCaseReceived.getCaseId()).thenReturn(CASE_ID);


        final CPPMessage existingCppMessage = new CPPMessage();
        existingCppMessage.setCaseId(randomUUID());
        existingCppMessage.setPtiUrn(randomUUID().toString());
        existingCppMessage.setOiId(OI_ID);
        when(cppMessageRepository.findBy(any())).thenReturn(existingCppMessage);

        spiProsecutionCaseRecivedListener.spiProsecutionCaseReceivedListener(spiProsecutionCaseReceivedEnvelope);
        verify(cppMessageRepository).save(any(CPPMessage.class));
    }
}
