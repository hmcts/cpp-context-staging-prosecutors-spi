package uk.gov.moj.cpp.casefilter.azure.service;

import static java.lang.System.getenv;

import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

@SuppressWarnings({"squid:S2629", "squid:S2221"})
public class PCFQueryService {
    private static final String BASE_URI =  getenv("INTEGRATION_HOST_URI");
    private static final String PCF_QUERY_BY_REF_ENDPOINT = BASE_URI + "/prosecutioncasefile-query-api/query/api/rest/prosecutioncasefile/cases?prosecutionCaseReference=%s";
    private static final String CASES_MEDIA_TYPE = "application/vnd.prosecutioncasefile.query.case-by-prosecutionCaseReference+json";
    private static final String USER_ID = "CJSCPPUID";
    private static final String USER_ID_VALUE = "pcfuser-id";

    private final HttpClientWrapper httpClientWrapper = new HttpClientWrapper();

    public boolean isCaseExistsInPCF(final String caseReference, final Logger logger) {

        final String endPointUrl = String.format(PCF_QUERY_BY_REF_ENDPOINT, caseReference);

        logger.info(String.format("PCF End point url %s", endPointUrl ));

        final HttpGet httpGet = new HttpGet(endPointUrl);
        httpGet.addHeader("Accept", CASES_MEDIA_TYPE);
        httpGet.addHeader(USER_ID, getenv(USER_ID_VALUE));

        try (CloseableHttpClient httpClient = httpClientWrapper.createSecureHttpClient();
             CloseableHttpResponse response = httpClient.execute(httpGet)) {

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                final String result = EntityUtils.toString(entity);
                if (result.contains(caseReference)) {
                    logger.info("Casereference exists in PCF");
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            logger.info(String.format("Error querying PCF for existing casereference %s , %s", caseReference, ex));
            return false;
        }
    }

}
