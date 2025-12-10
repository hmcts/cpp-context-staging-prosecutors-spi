package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpStatus.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CaseEjectedOrFilteredStatusByUrnFunctionTest {
    private CaseEjectedOrFilteredStatusByUrnFunction filteredStatusFunction;
    private HttpRequestMessage<Optional<String>> request;
    private AzureCloudStorageService azureCloudStorageService;
    private Logger logger;
    private ExecutionContext context;

    @BeforeEach
    public void setup() throws Exception {
        request = mock(HttpRequestMessage.class);
        azureCloudStorageService = mock(AzureCloudStorageService.class);
        filteredStatusFunction = new CaseEjectedOrFilteredStatusByUrnFunction();
        filteredStatusFunction.setAzureCloudStorageService(azureCloudStorageService);
        context = mock(ExecutionContext.class);

        logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
        doAnswer(answer ->  new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status((HttpStatus.OK)))
                .when(request).createResponseBuilder(any(HttpStatus.class));

    }

    @Test
    public void testWhenCaseIsEjectedOrFiltered() throws Exception {

        doReturn(Optional.of(readInMessage("cp20.xml"))).when(request).getBody();

        when(azureCloudStorageService.isCaseFilteredOrEjected( any(String.class), any(Logger.class))).thenReturn(true);

        final HttpResponseMessage response = filteredStatusFunction.isCaseEjectedOrFilteredByUrn(request, context);

        // Verify
        assertThat(response.getStatus(), is(OK));
        assertThat(response.getBody(), is(true));
    }


    @Test
    public void testWhenCaseIsNeitherEjectedNorFiltered() throws Exception {
        doReturn(Optional.of(readInMessage("cp20.xml"))).when(request).getBody();


        when(azureCloudStorageService.isCaseFilteredOrEjected(any(String.class), any(Logger.class))).thenReturn(false);

        final HttpResponseMessage response = filteredStatusFunction.isCaseEjectedOrFilteredByUrn(request, context);

        // Verify
        assertThat(response.getStatus(), is(OK));
        assertThat(response.getBody(), is(false));
    }

    @Test
    public void testBadRequestWhenEmptyQueryParameters() {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        doReturn(Optional.of("<xm></xml>")).when(req).getBody();
        final Map<String, String> queryParams = new HashMap<>();
        doReturn(queryParams).when(req).getQueryParameters();


        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));


        final HttpResponseMessage response = filteredStatusFunction.isCaseEjectedOrFilteredByUrn(req, context);

        // Verify
        assertEquals(OK, response.getStatus());
        assertEquals(false, response.getBody());
        assertThat(response.getStatus(), is(OK));
        assertThat(response.getBody(), is(false));
    }

    public String readInMessage(final String fileName) throws Exception {
        final InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        return IOUtils.toString(resourceAsStream);
    }

}