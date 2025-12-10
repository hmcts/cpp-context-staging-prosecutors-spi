package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;

import java.util.HashMap;
import java.util.Map;
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

public class CaseEjectedOrFilteredStatusFunctionTest {
    private static final String ROW_KEY ="AAAABBBBB_20";
    private static final String PROSECUTOR_CODE = "ProsecutorCode";
    private static final String CASE_REFERENCE = "CaseReference";
    private CaseEjectedOrFilteredStatusFunction filteredStatusFunction;
    private HttpRequestMessage<Optional<String>> request;
    private AzureCloudStorageService azureCloudStorageService;
    private Logger logger;
    private ExecutionContext context;

    @BeforeEach
    public void setup() {
        request = mock(HttpRequestMessage.class);
        azureCloudStorageService = mock(AzureCloudStorageService.class);
        filteredStatusFunction = new CaseEjectedOrFilteredStatusFunction();
        filteredStatusFunction.setAzureCloudStorageService(azureCloudStorageService);
        context = mock(ExecutionContext.class);

        logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
        doAnswer(answer ->  new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status((HttpStatus.OK)))
                .when(request).createResponseBuilder(any(HttpStatus.class));
    }

    @Test
    public void testWhenCaseIsEjectedOrFiltered() {

        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put(PROSECUTOR_CODE, "20");
        queryParams.put(CASE_REFERENCE, "ABCDEF22");
        doReturn(queryParams).when(request).getQueryParameters();

        when(azureCloudStorageService.isCaseFilteredOrEjected(any(String.class), any(String.class), any(Logger.class))).thenReturn(true);

        final HttpResponseMessage response = filteredStatusFunction.isCaseEjectedOrFiltered(request, context);

        // Verify
        assertEquals(OK, response.getStatus());
        assertEquals(true, response.getBody());
    }


    @Test
    public void testWhenCaseIsNeitherEjectedNorFiltered() {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put(PROSECUTOR_CODE, "20");
        queryParams.put(CASE_REFERENCE, "ABCDEF22");
        doReturn(queryParams).when(request).getQueryParameters();

        when(azureCloudStorageService.isCaseFilteredOrEjected(any(String.class), any(String.class), any(Logger.class))).thenReturn(false);

        final HttpResponseMessage response = filteredStatusFunction.isCaseEjectedOrFiltered(request, context);

        // Verify
        assertEquals(OK, response.getStatus());
        assertEquals(false, response.getBody());
    }

    @Test
    public void testBadRequestWhenEmptyQueryParameters() {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        doReturn(queryParams).when(req).getQueryParameters();


        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));


        final HttpResponseMessage response = filteredStatusFunction.isCaseEjectedOrFiltered(req, context);

        // Verify
        assertEquals(BAD_REQUEST, response.getStatus());
    }

}
