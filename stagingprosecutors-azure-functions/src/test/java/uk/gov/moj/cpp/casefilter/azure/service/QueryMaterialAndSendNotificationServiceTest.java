package uk.gov.moj.cpp.casefilter.azure.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.moj.cpp.casefilter.azure.pojo.ResponseDto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.json.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class QueryMaterialAndSendNotificationServiceTest {
    @Mock
    private CpsApiService cpsApiService;
    @Mock
    private Logger logger;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse closeableHttpResponse;
    @Mock
    private StatusLine statusLine;
    @Mock
    private CpsPayloadTransformService cpsPayloadTransformService;
    @Mock
    private HttpClientWrapper httpClientWrapper;
    @Mock
    private HttpEntity httpEntity;

    private QueryMaterialAndSendNotificationService target = new QueryMaterialAndSendNotificationService();

    @BeforeEach
    public void setUp() throws Exception {
        setField(target, "cpsApiService", cpsApiService);
        setField(target, "httpClientWrapper", httpClientWrapper);

        when(httpClientWrapper.createSecureHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
    }

    @Test
    public void shouldGetMaterialByIdSuccessfully() throws IOException {
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("foo".getBytes()));
        when(cpsApiService.sendMaterialNotification(any(), any(), any(), any(), any())).thenReturn((new ResponseDto(SC_OK, "Successful")));

        final ResponseDto responseDto = target.getMaterialById("materialId", "file.pdf", createObjectBuilder().build(), logger);

        verify(statusLine,times(2)).getStatusCode();
        verify(closeableHttpResponse,times(2)).getEntity();
        verify(cpsApiService).sendMaterialNotification(any(), any(), any(), any(), any(String.class));
        assertThat(responseDto.getMessageBody(), is("Successful"));
        assertThat(responseDto.getStatusCode(), is(SC_OK));
    }

    @Test
    public void shouldGetDefaultMaterialType() throws IOException {
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("foo".getBytes()));
        when(cpsApiService.sendMaterialNotification(any(), any(), any(), any(), eq("application/pdf"))).thenReturn((new ResponseDto(SC_OK, "Successful")));

        final ResponseDto responseDto = target.getMaterialById("materialId", "file.pdf", createObjectBuilder().build(), logger);

        verify(statusLine,times(2)).getStatusCode();
        verify(closeableHttpResponse,times(2)).getEntity();
        verify(cpsApiService).sendMaterialNotification(any(), any(), any(), any(), eq("application/pdf"));
        assertThat(responseDto.getMessageBody(), is("Successful"));
        assertThat(responseDto.getStatusCode(), is(SC_OK));
    }

    @Test
    public void shouldGetProvidedMaterialType() throws IOException {
        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("foo".getBytes()));
        when(cpsApiService.sendMaterialNotification(any(), any(), any(), any(), eq("application/msword"))).thenReturn((new ResponseDto(SC_OK, "Successful")));

        final ResponseDto responseDto = target.getMaterialById("materialId", "file.pdf", createObjectBuilder()
                .add("materialNotification", createObjectBuilder().add("materialContentType", "application/msword").build())
                .build(), logger);

        verify(statusLine,times(2)).getStatusCode();
        verify(closeableHttpResponse,times(2)).getEntity();
        verify(cpsApiService).sendMaterialNotification(any(), any(), any(), any(), eq("application/msword"));
        assertThat(responseDto.getMessageBody(), is("Successful"));
        assertThat(responseDto.getStatusCode(), is(SC_OK));
    }

    @Test
    public void shouldGetMaterialByIdThrowExceptionForNon2020ResponseCode() {
        when(statusLine.getStatusCode()).thenReturn(SC_BAD_REQUEST);

        assertThrows(RuntimeException.class, () -> target.getMaterialById("materialId", "file.pdf", createObjectBuilder().build(), logger));
    }
}