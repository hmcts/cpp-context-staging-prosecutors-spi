package uk.gov.moj.cpp.casefilter.azure.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.storage.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.casefilter.azure.pojo.ProsecutorForPTIUrn;
import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;
import uk.gov.moj.cpp.casefilter.azure.service.RefDataQueryService;

import java.util.Optional;
import java.util.logging.Logger;

import static com.microsoft.azure.functions.HttpStatus.OK;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings({"squid:S1312"})
public class GetProsecutorOucodeForPtiUrnFunctionTest {

    private GetProsecutorOucodeForPtiUrnFunction getProsecutorOucodeForPtiUrnFunction;
    private  AzureCloudStorageService azureCloudStorageService;
    private RefDataQueryService refDataQueryService;
    private ExecutionContext context;
    private HttpRequestMessage<Optional<String>> request;
    private Logger logger = null;


    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() throws StorageException {
        request = mock(HttpRequestMessage.class);
        getProsecutorOucodeForPtiUrnFunction = new GetProsecutorOucodeForPtiUrnFunction();
        azureCloudStorageService = mock(AzureCloudStorageService.class);
        refDataQueryService = mock(RefDataQueryService.class);
        getProsecutorOucodeForPtiUrnFunction.setRefDataQueryService(refDataQueryService);
        logger = mock(Logger.class);
        context = mock(ExecutionContext.class);
        final Logger logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
        doAnswer(answer ->  new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status((HttpStatus.OK)).header(CONTENT_TYPE, APPLICATION_JSON))
                .when(request).createResponseBuilder(any(HttpStatus.class));

    }

    @Test
    public void shouldReturnResponseContainingFalseForUrnNotPresent(){
        when(request.getBody()).thenReturn(Optional.of("abc"));
        final HttpResponseMessage response =  getProsecutorOucodeForPtiUrnFunction.getProsecutorOucodeForPtiUrnFunction(request, context);
        assertEquals(OK, response.getStatus());
        assertEquals(false, response.getBody());
    }

    @Test
    public void shouldReturnResponseContainingFalseForEmptyB0dy(){
        when(request.getBody()).thenReturn(Optional.empty());
        final HttpResponseMessage response =  getProsecutorOucodeForPtiUrnFunction.getProsecutorOucodeForPtiUrnFunction(request, context);
        assertEquals(OK, response.getStatus());
        assertEquals(false, response.getBody());
    }

    @Test
    public void shouldReturnResponseContainingOucodeWhenProsecutorFound() throws Exception {
        when(refDataQueryService.getProsecutorOucodeForPtiUrn(anyString(), any())).thenReturn("0450000");
        when(request.getBody()).thenReturn(Optional.of("<URN>45MD0000220</URN>"));
        final HttpResponseMessage response =  getProsecutorOucodeForPtiUrnFunction.getProsecutorOucodeForPtiUrnFunction(request, context);
        assertEquals(OK, response.getStatus());
        final ProsecutorForPTIUrn prosecutorForPTIUrn = (ProsecutorForPTIUrn) response.getBody();
        assertEquals("0450000", prosecutorForPTIUrn.getOucode());
    }

}

