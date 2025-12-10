package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;
import uk.gov.moj.cpp.casefilter.azure.service.EventGridService;
import uk.gov.moj.cpp.casefilter.azure.utils.FileUtils;

import java.util.Optional;
import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SetCaseEjectedFunctionTest {

    private SetCaseEjectedFunction setCaseEjectedFunction;
    private AzureCloudStorageService azureCloudStorageService;
    private HttpRequestMessage<Optional<String>> request;
    private EventGridService eventGridService;

    private static final String ERROR_MESSAGE = "Please pass all parameters (CaseId, ProsecutorOUCode, CaseInitiationCode, CaseReference) with payload";

    @BeforeEach
    public void setup() {
        setCaseEjectedFunction = new SetCaseEjectedFunction();

        azureCloudStorageService = mock(AzureCloudStorageService.class);
        request = mock(HttpRequestMessage.class);
        eventGridService = mock(EventGridService.class);
        setCaseEjectedFunction.setEventGridService(eventGridService);
        setCaseEjectedFunction.setAzureCloudStorageService(azureCloudStorageService);
        doNothing().when(eventGridService).publishEventsToCourtStore(anyString(),any(Logger.class));
        doAnswer(answer -> new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status((HttpStatus.OK)))
                .when(request).createResponseBuilder(any(HttpStatus.class));
    }

    @Test
    public void testCaseEjectedSuccessfully() throws Exception {
        String payload = FileUtils.getPayload("SetCaseEjected.json");

        when(request.getBody()).thenReturn(Optional.of(payload));
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        doAnswer(answer -> new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status((HttpStatus.OK)))
                .when(request).createResponseBuilder(any(HttpStatus.class));

        final HttpResponseMessage ret = setCaseEjectedFunction.setCaseEjected(request, context);

        verify(azureCloudStorageService).createOrUpdateEjectedOrFilteredCase(any());
        assertThat(ret.getStatus(), is(OK));
    }

    @Test
    public void testCaseEjectedWhenPayloadIsEmpty() throws Exception {
        String payload = "{}";

        when(request.getBody()).thenReturn(Optional.of(payload));
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        doAnswer(answer -> new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status((HttpStatus.BAD_REQUEST)))
                .when(request).createResponseBuilder(any(HttpStatus.class));

        final HttpResponseMessage ret = setCaseEjectedFunction.setCaseEjected(request, context);

        assertThat(ret.getStatus(), is(BAD_REQUEST));
        assertThat(ret.getBody(), is(ERROR_MESSAGE));
    }
}
