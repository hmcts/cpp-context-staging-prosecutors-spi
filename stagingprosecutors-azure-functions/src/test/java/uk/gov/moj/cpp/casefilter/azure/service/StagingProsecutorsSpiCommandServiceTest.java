package uk.gov.moj.cpp.casefilter.azure.service;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsSpiCommandServiceTest {

    private final StagingProsecutorsSpiCommandService stagingProsecutorsSpiCommandService = new StagingProsecutorsSpiCommandService();

    private HttpClientWrapper httpClientWrapper = mock(HttpClientWrapper.class);
    private Logger logger = mock(Logger.class);
    private CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    private CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    private StatusLine statusLine = mock(StatusLine.class);

    @Captor
    private ArgumentCaptor<HttpPost> argumentCaptor;

    @BeforeEach
    public void setUp() {
        ReflectionUtil.setField(stagingProsecutorsSpiCommandService, "httpClientWrapper", httpClientWrapper);
    }

    @Test
    public void shouldMakeACallToStagingProsecutorsSpi() throws IOException {
        when(httpClientWrapper.createSecureHttpClient()).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_ACCEPTED);

        stagingProsecutorsSpiCommandService.filterProsecutionCaseInStagingProsecutorSpi("ptiUrn", logger);

        verify(httpClient,times(1)).execute(argumentCaptor.capture());
        final HttpPost httpPost = argumentCaptor.getValue();

        // assert right headers and payload are sent
        assertThat(httpPost.getHeaders(CONTENT_TYPE).length, equalTo(1));
        assertThat(httpPost.getHeaders(CONTENT_TYPE)[0].getValue(), equalTo("application/vnd.stagingprosecutorsspi.command.spi.filter-prosecution-case+json"));
        assertThat(httpPost.getHeaders("CJSCPPUID").length, equalTo(1));
        assertThat(httpPost.getEntity(), notNullValue());
        assertThat(httpPost.getEntity(), instanceOf(StringEntity.class));
    }

}