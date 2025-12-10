package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.casefilter.azure.utils.FileUtils;

import java.util.Optional;
import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class RelayCaseToLibraFunctionTest {

    private RelayCaseToLibraFunction relayCaseToLibraFunction;

    private ExecutionContext context;

    @BeforeEach
    public void setup() {
        context = mock(ExecutionContext.class);
        final Logger logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
        relayCaseToLibraFunction = new RelayCaseToLibraFunction();
    }

    @Test
    public void shouldReturnCJSEMetadataWithChangedHeader() throws Exception {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(Optional.of(FileUtils.getPayload("SPI_In_Message"))).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));



        // Invoke
        final HttpResponseMessage ret = relayCaseToLibraFunction.relayCaseToLibra(req, context);
        // Verify
        assertEquals(OK, ret.getStatus());
        final String relayResponse = (String) ret.getBody();
        assertTrue(relayResponse.contains("<NS2:DestinationID>Z00CJSE</NS2:DestinationID>"));
        assertTrue(relayResponse.contains("<NS2:SourceID>C00CommonPlatform</NS2:SourceID>"));
        assertTrue(relayResponse.contains("&lt;RouteSourceSystem literalvalue=\"String\"&gt;C00CommonPlatform&lt;/RouteSourceSystem&gt;&lt;"));
    }

    @Test
    public void shouldReturnBadRequestWhenNoDestinationIDPresent() throws Exception {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(Optional.of(FileUtils.getPayload("SPI_In_Message_invalid"))).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage ret = relayCaseToLibraFunction.relayCaseToLibra(req, context);
        final String relayResponse = (String) ret.getBody();

        // Verify
        assertEquals(BAD_REQUEST, ret.getStatus());
        assertEquals("{\"mdifailure\":true}", relayResponse);
    }

    @Test
    public void shouldReturnBadRequestWhenNoBodyMessageFound() throws Exception {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(Optional.of("")).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage ret = relayCaseToLibraFunction.relayCaseToLibra(req, context);
        final String relayResponse = (String) ret.getBody();

        // Verify
        assertEquals(BAD_REQUEST, ret.getStatus());
        assertEquals("{\"mdifailure\":true}", relayResponse);
    }
}
