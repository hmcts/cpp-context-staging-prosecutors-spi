
package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import uk.gov.moj.cpp.staging.prosecutors.spi.events.CPPMessagePreparedForSending;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.CPPMessageResendFailed;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.UnsuccessfulSyncResponseRecorded;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CppMessageSentSucceessfullyRecorded;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.RetryDelayRequired;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SyncResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CPPMessageTest {

    @Test
    public void shouldCreateCorrectEventWhenPreparingCPPMessageForProcessing() {
        final CPPMessage cppMessage = new CPPMessage();
        Stream<Object> eventStream = cppMessage.prepareCPPMessageForSending("messageID", "message", "sourceId", "destination");
        List<?> eventList = eventStream.collect(Collectors.toList());
        isCorrectEventType(CPPMessagePreparedForSending.class, eventList);
    }

    @Test
    public void shouldCreateCorrectEventWhenSuccessfullyManuallyResendingMessage() {
        final CPPMessage cppMessage = setupCPPMessage();
        Stream<Object> eventStream = cppMessage.resendMessage(randomUUID());
        List<?> eventList = eventStream.collect(Collectors.toList());
        isCorrectEventType(CPPMessagePreparedForSending.class, eventList);
    }

    @Test
    public void shouldCreateCorrectEventWhenFailedToManuallyResendMessage() {
        UUID cppMessageId= randomUUID();
        Stream<Object> eventStream = new CPPMessage().resendMessage(cppMessageId);
        List<?> eventList = eventStream.collect(Collectors.toList());
        isCorrectEventType(CPPMessageResendFailed.class, eventList);

        CPPMessageResendFailed cppMessageResendFailed = (CPPMessageResendFailed) eventList.get(0);
        assertEquals(cppMessageId, cppMessageResendFailed.getId(),"Unexpected UUID");
    }

    @Test
    public void shouldCreateCorrectEventWhenSuccessfullySendingMessageForRetry() {
        final CPPMessage cppMessage = setupCPPMessage();
        Stream<Object> eventStream = cppMessage.sendMessageForRetry(5);
        List<?> eventList = eventStream.collect(Collectors.toList());
        isCorrectEventType(CPPMessagePreparedForSending.class, eventList);
    }

    @Test
    public void shouldCreateCorrectEventWhenReceiveSyncResponseWithResponseCode1() {
        final CPPMessage cppMessage = setupCPPMessage();
        Stream<Object> eventStream = cppMessage.receiveSyncResponse(SyncResponse.syncResponse().withResponseCode(1).build(), 5);
        List<?> eventList = eventStream.collect(Collectors.toList());
        isCorrectEventType(CppMessageSentSucceessfullyRecorded.class, eventList);
    }

    @Test
    public void shouldCreateCorrectEventWhenReceiveSyncResponseWithUnsuccessfulSubmission() {
        final CPPMessage cppMessage = setupCPPMessage();
        Stream<Object> eventStream = cppMessage.receiveSyncResponse(SyncResponse.syncResponse().withResponseCode(0).build(), -1);
        List<?> eventList = eventStream.collect(Collectors.toList());
        isCorrectEventType(UnsuccessfulSyncResponseRecorded.class, eventList);
    }

    @Test
    public void shouldCreateCorrectEventWhenReceiveSyncResponseWithUnsuccessfulSubmissionAndRetryResponseListContains200CodeAndSendTriesUnderLimit() {
        final CPPMessage cppMessage = setupCPPMessage();
        Stream<Object> eventStream = cppMessage.receiveSyncResponse(SyncResponse.syncResponse().withResponseCode(200).build(), 5);
        List<?> eventList = eventStream.collect(Collectors.toList());

        assertEquals(2, eventList.size(), "Unexpected number of events");
        assertEquals(UnsuccessfulSyncResponseRecorded.class, eventList.get(0).getClass(), "Unexpected event type");
        assertEquals(RetryDelayRequired.class, eventList.get(1).getClass(), "Unexpected event type");

    }

    private CPPMessage setupCPPMessage() {
        final CPPMessage cppMessage = new CPPMessage();
        cppMessage.prepareCPPMessageForSending("messageID", "message", "sourceId", "destination");
        return cppMessage;
    }

    private void isCorrectEventType(Class classType, List<?> eventList) {
        assertEquals(1, eventList.size(), "Unexpected number of events");
        assertEquals(classType, eventList.get(0).getClass(), "Unexpected event type");
    }

}