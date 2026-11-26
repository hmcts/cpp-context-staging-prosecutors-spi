package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpStatus.OK;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.storage.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetFilteredCasesCountByProsecutorFunctionTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpRequestMessage<Optional<String>> request;

    @Mock
    private HttpResponseMessage response;

    @Mock
    private ExecutionContext context;

    @Mock
    private AzureCloudStorageService azureCloudStorageService;

    @InjectMocks
    private GetFilteredCasesCountByProsecutorFunction getFilteredCasesCountByProsecutorFunction;

    @BeforeEach
    public void setUp() {
        when(request.createResponseBuilder(any()).header(CONTENT_TYPE, APPLICATION_JSON).body(any()).build()).thenReturn(response);
        when(context.getLogger()).thenReturn(Logger.getGlobal());
        when(response.getStatus()).thenReturn(OK);
    }

    @Test
    public void ShouldTestGetFilteredCasesCountByProsecutorFunction() throws StorageException {
        final Map<String, Long> filteredCaseCountByProsecutor = new HashMap<>();
        when(azureCloudStorageService.getDailyFilteredCaseCountByProsecutor()).thenReturn(filteredCaseCountByProsecutor);
        when(azureCloudStorageService.getTotalFilteredCaseCountByProsecutor()).thenReturn(filteredCaseCountByProsecutor);
        final HttpResponseMessage response = getFilteredCasesCountByProsecutorFunction.getFilteredCasesCountByProsecutor(request, context);
        verify(azureCloudStorageService).getDailyFilteredCaseCountByProsecutor();
        verify(azureCloudStorageService).getTotalFilteredCaseCountByProsecutor();
        assertThat(response.getStatus(), is(OK));
    }
}