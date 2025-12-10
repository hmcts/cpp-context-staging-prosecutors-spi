package uk.gov.moj.cpp.casefilter.azure.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.storage.StorageException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import uk.gov.moj.cpp.casefilter.azure.exception.AzureStorageException;
import uk.gov.moj.cpp.casefilter.azure.pojo.CaseFilterRule;
import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;
import uk.gov.moj.cpp.casefilter.azure.service.PCFQueryService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.microsoft.azure.functions.HttpMethod.GET;
import static com.microsoft.azure.functions.HttpStatus.OK;
import static com.microsoft.azure.functions.annotation.AuthorizationLevel.FUNCTION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"squid:S1312", "squid:S1166", "squid:S2629", "squid:S2677"})
public class CheckProsecutorIsLiveFunctionTest {

    public static final String A_CASE_REFERENCE = "ABCDEF22";
    public static final boolean FILTERED_IN = false;
    public static final boolean FILTERED_OUT = true;
    private static final String COURT_CENTRE_CODE = "CourtCentreCode";
    private static final String OUCODE = "oucode";
    private static final String INITIATION_CODE = "InitiationCode";
    private static final String CASE_REFERENCE = "CaseReference";
    private static final String DATE_OF_HEARING = "DateOfHearing";
    private static final String TIME_OF_HEARING = "TimeOfHearing";
    private static final String SUMMONS_CODE = "SummonsCode";
    private CheckProsecutorIsLiveFunction checkProsecutorIsLiveFunction;
    private AzureCloudStorageService azureCloudStorageService;
    private ExecutionContext context;
    private HttpRequestMessage<Optional<String>> req;
    private HashMap<Object, Object> queryParams;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() throws StorageException {
        checkProsecutorIsLiveFunction = new CheckProsecutorIsLiveFunction();
        azureCloudStorageService = mock(AzureCloudStorageService.class);
        checkProsecutorIsLiveFunction.setAzureCloudStorageService(azureCloudStorageService);
        context = mock(ExecutionContext.class);
        final Logger logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
        doReturn(readFileAsStream("liveCourtsFile.csv")).when(azureCloudStorageService).readRemoteFile();
        req = mock(HttpRequestMessage.class);
        queryParams = new HashMap<>();

    }


    @Test
    public void shouldReturnFalseForNullOucode(){
        doRequest(req);
        final HttpResponseMessage response = checkProsecutorIsLiveFunction.checkProsecutorIsLive(req, context);
        assertEquals(OK, response.getStatus());
        assertEquals(false, response.getBody());
    }

    @Test
    public void shouldReturnFalseForNotFoundOucode(){
        queryParams.put(OUCODE, "AAAAAA");
        doRequest(req);
        final HttpResponseMessage response = checkProsecutorIsLiveFunction.checkProsecutorIsLive(req, context);
        assertEquals(OK, response.getStatus());
        assertEquals(false, response.getBody());
    }
    @Test
    public void shouldReturnTrueForMatchedOucode(){
        queryParams.put(OUCODE, "303");
        doRequest(req);
        doReturn(queryParams).when(req).getQueryParameters();
        final HttpResponseMessage response = checkProsecutorIsLiveFunction.checkProsecutorIsLive(req, context);
        assertEquals(OK, response.getStatus());
        assertEquals(true, response.getBody());
    }
    public InputStream readFileAsStream(final String fileName) {
        return this.getClass().getClassLoader().getResourceAsStream(fileName);
    }
    private void doRequest(final HttpRequestMessage<Optional<String>> req) {
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));
    }
}
