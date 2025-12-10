package uk.gov.moj.cpp.casefilter.azure.service;

import static java.lang.String.format;
import static java.lang.System.getenv;

import uk.gov.moj.cpp.casefilter.azure.exception.CourtStoreException;
import uk.gov.moj.cpp.casefilter.azure.utils.FileUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.json.JsonObject;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

@SuppressWarnings({"squid:S2629", "squid:S2139"})
public class NotifyCourtStoreService {

    private static final String REQUEST_BODY_PATTERN = "{ \"urn\" : \"%s\" , \"operation\" : \"%s\"} ";
    private static final String COURT_STORE_ENDPOINT = getenv("CourtStoreURL");
    private static final String COURT_STORE_APIM_KEY = getenv("COURT_STORE_APIM_KEY");
    private static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
    private static final String DATA = "data";
    private static final int DEFAULT_MAX_RETRY_COUNT = 3;
    private static final int DEFAULT_RETRY_INTERVAL_MILLIS = 1000;
    private static final Integer MAX_RETRY_COUNT = getenv("APIM_RETRY_COUNT") != null ? Integer.valueOf(getenv("APIM_RETRY_COUNT")) : DEFAULT_MAX_RETRY_COUNT;
    private static final Integer RETRY_INTERVAL_MILLIS = getenv("APIM_RETRY_INTERVAL") != null ? Integer.valueOf(getenv("APIM_RETRY_INTERVAL")) : DEFAULT_RETRY_INTERVAL_MILLIS;
    private static final int ERROR_CODE = 429;

    private final HttpClientWrapper httpClientWrapper = new HttpClientWrapper();

    public void notifyCourtStore(String caseURN, final Logger logger) {
        performCourtStoreApimCallWithRetry(logger, caseURN, "CaseEjected");
    }

    public void relayCase(final String req, final Logger logger) {
        final JsonObject jsonObj = FileUtil.getJsonObject(req);
        final String caseReference = jsonObj.getString(DATA, null);
        performCourtStoreApimCallWithRetry(logger, caseReference, "CaseOnCpp");
    }

    private void performCourtStoreApimCallWithRetry(final Logger logger, final String caseReference, final String operation) {
        logger.info(format("Court Store end point url %s", COURT_STORE_ENDPOINT));
        final HttpPost httpPost = new HttpPost(COURT_STORE_ENDPOINT);
        final String requestBody = format(REQUEST_BODY_PATTERN, caseReference, operation);
        logger.info(format("Request body : %s", requestBody));
        httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
        httpPost.setHeader(OCP_APIM_SUBSCRIPTION_KEY, COURT_STORE_APIM_KEY);
        final AtomicInteger retryCount = new AtomicInteger(0);

        while (retryCount.get() <= MAX_RETRY_COUNT) {

            try (CloseableHttpClient httpClient = httpClientWrapper.createSecureHttpClient();
                 CloseableHttpResponse response = httpClient.execute(httpPost)) {
                final int httpStatusCode = response.getStatusLine().getStatusCode();
                final String responseBody = EntityUtils.toString(response.getEntity());
                logger.info(format("APIM Response code : %s, Response body : %s", httpStatusCode, responseBody));

                if (httpStatusCode > ERROR_CODE) {
                    performRetry(logger, retryCount);
                } else {
                    break;
                }
            } catch (IOException e) {
                logger.info(format("Error relaying case reference to court store %s , %s", caseReference, e));
                performRetry(logger, retryCount);
            }
        }
    }

    private void performRetry(final Logger logger, AtomicInteger retryCount) {
        if (retryCount.incrementAndGet() <= MAX_RETRY_COUNT) {
            logger.info(format("Retry count %s ", retryCount.get()));
            try {
                Thread.sleep(RETRY_INTERVAL_MILLIS);
            } catch (InterruptedException ignored) {
                logger.info(format("InterruptedException: %s ", ignored));
                Thread.currentThread().interrupt();
            }
        } else {
            //Exhausted all retries, rethrow the exception
            throw new CourtStoreException("Error sending message to court store. Failed to execute APIM " + COURT_STORE_ENDPOINT + " call after " + MAX_RETRY_COUNT + " retries");
        }
    }

}
