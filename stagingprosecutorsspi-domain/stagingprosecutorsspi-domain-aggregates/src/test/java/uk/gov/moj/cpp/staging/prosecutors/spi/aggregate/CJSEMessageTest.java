package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseDuplicateRequestMessageReceived;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseMessageReceivedWithMdiErrors;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseRequestMessageReceived;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiError;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CJSEMessageTest {

    private static final String REQUEST_ID = randomUUID().toString();
    private static final String MESSAGE = "Message";

    @Mock
    private CjseMessage cjseMessage;

    @Mock
    SpiError error;

    private CJSEMessage cjseMessageAggregate;

    private List<?> eventList;

    public void setup() {
        when(cjseMessage.getRequestId()).thenReturn(REQUEST_ID);
        when(cjseMessage.getMessage()).thenReturn(MESSAGE);

        cjseMessageAggregate = new CJSEMessage();
        final Stream<Object> eventStream = cjseMessageAggregate.receiveCjseMessage(this.cjseMessage);
        eventList = eventStream.collect(Collectors.toList());
    }

    public void setupWithError() {
        when(cjseMessage.getRequestId()).thenReturn(REQUEST_ID);
        when(cjseMessage.getMdiError()).thenReturn(error);

        cjseMessageAggregate = new CJSEMessage();
        final Stream<Object> eventStream = cjseMessageAggregate.receiveCjseMessage(this.cjseMessage);
        eventList = eventStream.collect(Collectors.toList());
    }

    @Test
    public void shouldCreateCorrectEventWhenReceivingCjseMessageWithError() {
        setupWithError();
        assertEquals(1, eventList.size());
        assertEquals(CjseMessageReceivedWithMdiErrors.class, eventList.get(0).getClass());
    }

    @Test
    public void shouldCreateCorrectEventWhenReceivingCjseMessage() {
        setup();
        assertEquals(1, eventList.size());
        assertEquals(CjseRequestMessageReceived.class, eventList.get(0).getClass());
    }

    @Test
    public void shouldCreateEventWithCorrectRequestIDAndMessageWhenReceivingCjseMessage(){
        setup();
        final CjseRequestMessageReceived cjseRequestMessageReceived= (CjseRequestMessageReceived) eventList.get(0);

        assertThat(cjseRequestMessageReceived.getRequestId(), is(REQUEST_ID));
        assertThat(cjseRequestMessageReceived.getCjseMessage().getMessage(), is(MESSAGE));
    }

    @Test
    public void shouldCreateDuplicateCjseMessageRecdEventWhenDuplicateMessageRecd(){
        setup();
        eventList = cjseMessageAggregate.receiveCjseMessage(cjseMessage).collect(Collectors.toList());
        final CjseDuplicateRequestMessageReceived cjseDuplicateRequestMessageReceived = (CjseDuplicateRequestMessageReceived) eventList.get(0);

        assertThat(cjseDuplicateRequestMessageReceived.getRequestId(), is(REQUEST_ID));
        assertThat(cjseDuplicateRequestMessageReceived.getCjseMessage().getMessage(), is(MESSAGE));
    }
}
