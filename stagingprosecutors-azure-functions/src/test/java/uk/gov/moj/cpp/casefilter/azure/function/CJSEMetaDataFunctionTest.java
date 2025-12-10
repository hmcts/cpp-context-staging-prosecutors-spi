package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.casefilter.azure.pojo.CJSEMetaDataResponse;

import java.io.InputStream;
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

public class CJSEMetaDataFunctionTest {

    private CJSEMetaDataFunction cjseMetaData;

    private ExecutionContext context;


    @BeforeEach
    public void setup() {
        context = mock(ExecutionContext.class);
        final Logger logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
        cjseMetaData = new CJSEMetaDataFunction();
    }


    @Test
    public void shouldReturnCJSEMetadataWithReponseTrue_whenAsyncResponseReceived() throws Exception {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(Optional.of(readSpiInMessage("SPI_In_Message_Async_Response"))).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage ret = cjseMetaData.cjseMetaData(req, context);

        // Verify
        assertEquals(OK, ret.getStatus());

        CJSEMetaDataResponse cjseMetaDataResponse = (CJSEMetaDataResponse) ret.getBody();
        assertEquals(true, cjseMetaDataResponse.getCjseMetaData().getIsAsyncResponse());
        assertEquals(false, cjseMetaDataResponse.getCjseMetaData().isMdiFailure());
        assertNull(cjseMetaDataResponse.getCjseMetaData().getSummons());
    }

    @Test
    public void shouldReturnCJSEMetadataResponseSuccess_whenCaseInitiationIsPresent() throws Exception {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(Optional.of(readSpiInMessage("SPI_In_Message"))).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage ret = cjseMetaData.cjseMetaData(req, context);

        // Verify
        assertEquals(OK, ret.getStatus());

        CJSEMetaDataResponse cjseMetaDataResponse = (CJSEMetaDataResponse) ret.getBody();
        assertEquals("83GD31317122", cjseMetaDataResponse.getCjseMetaData().getCaseReference());
        assertEquals("J", cjseMetaDataResponse.getCjseMetaData().getCaseInitiationCode());
        assertEquals("B01BH00", cjseMetaDataResponse.getCjseMetaData().getProsecutorOUCode());
        assertEquals("B01LY01", cjseMetaDataResponse.getCjseMetaData().getCourtCenterOUCode());
        assertEquals("2019-10-15", cjseMetaDataResponse.getCjseMetaData().getDateOfHearing());
        assertEquals("09:01:01.001", cjseMetaDataResponse.getCjseMetaData().getTimeOfHearing());
        assertNull(cjseMetaDataResponse.getCjseMetaData().getSummons());
    }

    @Test
    public void shouldReturnCJSEMetadataResponseSuccessWhenCaseInitiationIsPresentAndXmlContainsElementsWithNamespacePrefix() throws Exception {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(Optional.of(readSpiInMessage("spi-in-with-namespace-prefix.xml"))).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage ret = cjseMetaData.cjseMetaData(req, context);

        // Verify
        assertEquals(OK, ret.getStatus());

        CJSEMetaDataResponse cjseMetaDataResponse = (CJSEMetaDataResponse) ret.getBody();
        assertEquals("50CH0000122", cjseMetaDataResponse.getCjseMetaData().getCaseReference());
        assertEquals("Q", cjseMetaDataResponse.getCjseMetaData().getCaseInitiationCode());
        assertEquals("150DE00", cjseMetaDataResponse.getCjseMetaData().getProsecutorOUCode());
        assertEquals("B50FI01", cjseMetaDataResponse.getCjseMetaData().getCourtCenterOUCode());
        assertEquals("2023-01-25", cjseMetaDataResponse.getCjseMetaData().getDateOfHearing());
        assertEquals("10:00:00+00:00", cjseMetaDataResponse.getCjseMetaData().getTimeOfHearing());
        assertNull(cjseMetaDataResponse.getCjseMetaData().getSummons());
    }

    @Test
    public void shouldReturnCJSEMetadataResponseSuccess_andExtractSummonsCode_whenInitiationCodeIsSummons() throws Exception {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(Optional.of(readSpiInMessage("SPI_In_Message_with_summons"))).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage ret = cjseMetaData.cjseMetaData(req, context);

        // Verify
        assertEquals(OK, ret.getStatus());

        CJSEMetaDataResponse cjseMetaDataResponse = (CJSEMetaDataResponse) ret.getBody();
        assertEquals("83GD31317122", cjseMetaDataResponse.getCjseMetaData().getCaseReference());
        assertEquals("S", cjseMetaDataResponse.getCjseMetaData().getCaseInitiationCode());
        assertEquals("B01BH00", cjseMetaDataResponse.getCjseMetaData().getProsecutorOUCode());
        assertEquals("B01LY01", cjseMetaDataResponse.getCjseMetaData().getCourtCenterOUCode());
        assertEquals("2019-10-15", cjseMetaDataResponse.getCjseMetaData().getDateOfHearing());
        assertEquals("09:01:01.001", cjseMetaDataResponse.getCjseMetaData().getTimeOfHearing());
        assertEquals("A", cjseMetaDataResponse.getCjseMetaData().getSummons());
    }

    @Test
    public void shouldReturnCJSEMetadataResponseWithMdiFailure() throws Exception {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        doReturn(Optional.of(readSpiInMessage("SPI_In_Message_invalid"))).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage ret = cjseMetaData.cjseMetaData(req, context);

        // Verify
        assertEquals(OK, ret.getStatus());
        CJSEMetaDataResponse cjseMetaDataResponse = (CJSEMetaDataResponse) ret.getBody();
        assertEquals(true, cjseMetaDataResponse.getCjseMetaData().isMdiFailure());
    }

    public String readSpiInMessage(final String fileName) throws Exception {
        final InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        return IOUtils.toString(resourceAsStream);
    }
}
