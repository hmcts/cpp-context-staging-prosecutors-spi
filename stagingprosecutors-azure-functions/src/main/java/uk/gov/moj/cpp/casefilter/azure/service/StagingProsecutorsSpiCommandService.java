package uk.gov.moj.cpp.casefilter.azure.service;

import static java.lang.System.getenv;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

@SuppressWarnings({"squid:S2629", "squid:S2221"})
public class StagingProsecutorsSpiCommandService {
    private static final String BASE_URI =  getenv("INTEGRATION_HOST_URI");
    private static final String ENDPOINT = BASE_URI + getenv("StagingProsecutorsSpiURL");
    private static final String MEDIA_TYPE = "application/vnd.stagingprosecutorsspi.command.spi.filter-prosecution-case+json";
    private static final String USER_ID = "CJSCPPUID";
    private static final String USER_ID_VALUE = "pcfuser-id";
    HttpClientWrapper httpClientWrapper = new HttpClientWrapper();

    @SuppressWarnings({"squid:S2139", "squid:S00112", "squid:S2142"})
    public void filterProsecutionCaseInStagingProsecutorSpi(final String ptiUrn, final Logger logger) {

        final String endPointUrl = String.format(ENDPOINT, ptiUrn);

        logger.info(String.format("StagingProsecutorSpi End point url %s", endPointUrl));

        final HttpPost httpPost = new HttpPost(endPointUrl);

        httpPost.addHeader(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE);
        httpPost.addHeader(USER_ID, getenv(USER_ID_VALUE));
        StringEntity body = null;
        try {
            body = new StringEntity((new JSONObject()).toString());
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new RuntimeException(e);
        }
        httpPost.setEntity(body);
        boolean completed = false;
        int retry = 0;
        while(!completed && retry <= 3 ) {
            try (CloseableHttpClient httpClient = httpClientWrapper.createSecureHttpClient();
                 CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED) {
                    throw new RuntimeException("ProsecutionCaseInStagingProsecutorSpi call failed with http status code : " + response.getStatusLine().getStatusCode() + ", : response body :  " + EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
                }
                logger.info(String.format("StagingProsecutorSpi call successful with http status code : %s", response.getStatusLine().getStatusCode()));
                completed  = true;
            } catch (Exception ex) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(ex);
                }
                retry++;
                logger.log(Level.SEVERE, ex.getMessage());
                if(retry >= 3 ) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
}

