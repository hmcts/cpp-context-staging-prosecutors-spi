package uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.task;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveRetryDelayTimerFinished;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

@ServiceComponent(EVENT_PROCESSOR)
@Named()
public class ProcessTimerFinished implements JavaDelegate {

    @Inject
    private Sender sender;
    private static final String SPI_COMMAND_RECEIVE_TIMER_FINISHED_COMMAND = "stagingprosecutorsspi.command.handler.receive-retry-delay-timer-finished";

    @Handles("blah")
    @SuppressWarnings("squid:S1186")
    public void blah(final JsonEnvelope jsonEnvelope){

    }
    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
      final String userId = (String) delegateExecution.getVariable("userId");
      final UUID id = (UUID) delegateExecution.getVariable("cppMessageId");
      final Metadata metadataNew = metadataBuilder().withId(UUID.randomUUID()).withUserId(userId).withName(SPI_COMMAND_RECEIVE_TIMER_FINISHED_COMMAND)
              .build();
      final Envelope<ReceiveRetryDelayTimerFinished> mdiMessageEnvelope = envelopeFrom(metadataNew, new ReceiveRetryDelayTimerFinished(id));
        sender.send(mdiMessageEnvelope);

    }
}
