package uk.gov.moj.cpp.casefilter.azure.service;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.moj.cpp.casefilter.azure.pojo.ResponseDto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsApiServiceTest {

    @Mock
    private HttpClientWrapper httpClientWrapper;
    @Mock
    private Logger logger;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse closeableHttpResponse;
    @Mock
    private StatusLine statusLine;
    @Captor
    private ArgumentCaptor<HttpUriRequest> argumentCaptor;

    private final CpsApiService target = new CpsApiService();

    @BeforeEach
    public void setUp() throws Exception {
        setField(target, "httpClientWrapper", httpClientWrapper);

        when(httpClientWrapper.createSecureHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
    }

    @Test
    public void shouldSendMaterialNotificationSuccessfully() throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("Successful".getBytes());
        final BasicHttpEntity httpEntity = getHttpEntity(byteArrayInputStream);

        when(statusLine.getStatusCode()).thenReturn(SC_OK);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);

        final ResponseDto responseDto = target.sendMaterialNotification(createObjectBuilder().build(), getMaterialInputStream(), "file.pdf", logger, "text/plain");

        verify(httpClient).execute(argumentCaptor.capture());

        final HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) argumentCaptor.getValue();

        assertThat(request.getEntity(), notNullValue());
        assertThat(responseDto.getMessageBody(), is("Successful"));
        assertThat(responseDto.getStatusCode(), is(SC_OK));
    }

    @Test
    public void shouldSendMaterialNotificationFailForNon200ResponseCode() {
        final BasicHttpEntity httpEntity = getHttpEntity(new ByteArrayInputStream("Failed".getBytes()));

        when(statusLine.getStatusCode()).thenReturn(SC_BAD_REQUEST);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);

        final ResponseDto responseDto = target.sendMaterialNotification(createObjectBuilder().build(), getMaterialInputStream(), "file.pdf", logger, "text/plain");

        assertThat(responseDto.getMessageBody(), is("Failed"));
        assertThat(responseDto.getStatusCode(), is(SC_BAD_REQUEST));
    }

    @Test
    public void shouldSendMaterialNotification() {
        final BasicHttpEntity httpEntity = getHttpEntity(new ByteArrayInputStream("Failed".getBytes()));

        when(statusLine.getStatusCode()).thenReturn(SC_BAD_REQUEST);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);

        final ResponseDto responseDto = target.sendMaterialNotification(createObjectBuilder().build(), getMaterialInputStream(), "file.pdf", logger, "text/plain");

        assertThat(responseDto.getMessageBody(), is("Failed"));
        assertThat(responseDto.getStatusCode(), is(SC_BAD_REQUEST));
    }

    @AfterEach
    public void verifyInvocations() throws Exception {
        verify(statusLine).getStatusCode();
        verify(closeableHttpResponse).getEntity();
        verify(httpClientWrapper).createSecureHttpClient();
        verify(httpClient).execute(any(HttpUriRequest.class));
    }

    private BasicHttpEntity getHttpEntity(final InputStream inputStream) {
        final BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(inputStream);

        return httpEntity;
    }

    private ByteArrayInputStream getMaterialInputStream() {
        return new ByteArrayInputStream("File Content".getBytes());
    }
}