package uk.gov.moj.cpp.casefilter.azure.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import uk.gov.moj.cpp.casefilter.azure.service.EventGridService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class RelayCaseOnCPPToCourtStoreFunctionTest {


    private static final String CASE_REFERENCE = "CaseReference";
    private RelayCaseOnCPPToCourtStoreFunction relayCaseOnCPPToCourtStoreFunction;
    private HttpRequestMessage<Optional<String>> request;
    private EventGridService eventGridService;

    private Logger logger;
    private ExecutionContext context;
    private Optional<String>  requestBody;
    private Optional<String>  badRequestBody;

    @BeforeEach
    public void setup() {

        request = mock(HttpRequestMessage.class);
        requestBody = Optional.of(" { \"CaseReference\" : \"1234567890\" } ");
        badRequestBody = Optional.of(" {  } ");
        relayCaseOnCPPToCourtStoreFunction = new RelayCaseOnCPPToCourtStoreFunction();
        context = mock(ExecutionContext.class);
        eventGridService = mock(EventGridService.class);
        relayCaseOnCPPToCourtStoreFunction.setEventGridService(eventGridService);

        logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
        doNothing().when(eventGridService).publishCaseOnCPPEvents(anyString(),any(Logger.class));
        doAnswer(answer ->  new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status((HttpStatus.OK)))
                .when(request).createResponseBuilder(any(HttpStatus.class));

        when(request.getBody()).thenReturn(requestBody);
    }

    @Test
    public void testWhenCaseIsRelayed() {

        final Map<String, String> queryParams = new HashMap<>();
        final HttpResponseMessage response = relayCaseOnCPPToCourtStoreFunction.relayCaseOnCPPToCourtStore(request, context);
        // Verify
        assertEquals(OK, response.getStatus());
        assertEquals(true, response.getBody());
    }



    @Test
    public void testBadRequestWhenEmptyQueryParameters() {
        doAnswer(answer -> new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status((HttpStatus.BAD_REQUEST)))
                .when(request).createResponseBuilder(any(HttpStatus.class));
        final HttpResponseMessage response = relayCaseOnCPPToCourtStoreFunction.relayCaseOnCPPToCourtStore(request, context);
        // Verify
        assertEquals(BAD_REQUEST, response.getStatus());
    }

}

