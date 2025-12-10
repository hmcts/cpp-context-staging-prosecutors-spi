package uk.gov.moj.cpp.staging.prosecutors.spi.utils;


import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;

import uk.gov.justice.services.test.utils.core.rest.RestClient;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiremockTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(WiremockTestHelper.class);

    public static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    public static final String BASE_URI = System.getProperty("INTEGRATION_HOST_URI", "http://localhost:8080");

    private static final String WIREMOCK_PORT = System.getProperty("WIREMOCK_PORT", "8080");
    private static final String WIREMOCK_BASE_URI = BASE_URI.replace("8080", WIREMOCK_PORT);
    private static final String WIREMOCK_COUNT_URI = WIREMOCK_BASE_URI + "/__admin/requests/count";

    private static final RestClient restClient = new RestClient();

    public static void resetService() {
        configureFor(HOST, 8080);
        reset();
    }

    private static String buildGetHitsCountPayload(String url) {
        JSONObject jsonData = new JSONObject();
        jsonData.put("method", "POST");
        jsonData.put("urlPattern", url);
        return jsonData.toString();
    }

}
