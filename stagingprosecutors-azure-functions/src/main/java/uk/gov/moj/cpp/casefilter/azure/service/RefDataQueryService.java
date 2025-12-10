package uk.gov.moj.cpp.casefilter.azure.service;

import static java.lang.System.getenv;

import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

@SuppressWarnings({"squid:S2629", "squid:S2221","squid:S00112",  "squid:S1162"})
public class RefDataQueryService {
    private static final String BASE_URI =  getenv("INTEGRATION_HOST_URI");
    private static final String RED_DATA_QUERY_BY_PTIURN_ENDPOINT = BASE_URI + "/referencedata-query-api/query/api/rest/referencedata/prosecutor?ptiurn=%s";
    private static final String CASES_MEDIA_TYPE = "application/vnd.referencedata.query.prosecutor.by.ptiurn+json";
    private static final String USER_ID = "CJSCPPUID";
    private static final String USER_ID_VALUE = "pcfuser-id";

    private final HttpClientWrapper httpClientWrapper = new HttpClientWrapper();

    public String getProsecutorOucodeForPtiUrn(final String ptiURN, final Logger logger) throws Exception {

        final String endPointUrl = String.format(RED_DATA_QUERY_BY_PTIURN_ENDPOINT, ptiURN);

        logger.info(String.format("RefData End point url %s", endPointUrl ));

        final HttpGet httpGet = new HttpGet(endPointUrl);
        httpGet.addHeader("Accept", CASES_MEDIA_TYPE);
        httpGet.addHeader(USER_ID, getenv(USER_ID_VALUE));

        try (CloseableHttpClient httpClient = httpClientWrapper.createSecureHttpClient();
             CloseableHttpResponse response = httpClient.execute(httpGet)) {

            final int  statusCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_NOT_FOUND != statusCode) {
                logger.info("statusCode: " + statusCode);
                final HttpEntity entity = response.getEntity();
                final String result = EntityUtils.toString(entity);
                final JSONObject json = new JSONObject(result);
                logger.info(String.format("Entity %s", result));
                return json.getString("oucode");
                }
            logger.info("returning null");
            return null;
        }
    }

}
