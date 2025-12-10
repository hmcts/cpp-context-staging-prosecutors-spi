package uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.task;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveRetryDelayTimerFinished;
import org.activiti.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessorTimeFinishedTest {

    private static final String SPI_COMMAND_RECEIVE_TIMER_FINISHED_COMMAND = "stagingprosecutorsspi.command.handler.receive-retry-delay-timer-finished";

    @Mock
    private Sender sender;
    @Mock
    private DelegateExecution delegateExecution ;

    @InjectMocks
    private ProcessTimerFinished processTimerFinished;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;


    @Test
    public void shouldRaiseEventReceiveRetryDelayTimerFinished() throws Exception {

        when(delegateExecution.getVariable("userId")).thenReturn("userId");
        when(delegateExecution.getVariable("cppMessageId")).thenReturn(randomUUID());

        processTimerFinished.execute(delegateExecution);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope captorValue = envelopeArgumentCaptor.getValue();

        assertThat(captorValue.payload(), is(instanceOf(ReceiveRetryDelayTimerFinished.class)));
        assertThat(captorValue.metadata().name(), is(SPI_COMMAND_RECEIVE_TIMER_FINISHED_COMMAND));

    }

}