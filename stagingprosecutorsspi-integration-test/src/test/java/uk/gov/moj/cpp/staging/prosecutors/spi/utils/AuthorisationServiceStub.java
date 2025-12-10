package uk.gov.moj.cpp.staging.prosecutors.spi.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;

import java.util.UUID;

import javax.json.Json;

public class AuthorisationServiceStub {

    private static final String CAPABILITY_ENABLEMENT_QUERY_URL = "/authorisation-service-server/rest/capabilities/%s";
    private static final String CAPABILITY_ENABLEMENT_QUERY_MEDIA_TYPE = "application/vnd.authorisation.capability+json";
    private static final String AUTHORISATION_SERVICE_SERVER = "authorisation-service-server";

    public static void stubSetStatusForCapability(String capabilityName, boolean statusToReturn) {
        String url = format(CAPABILITY_ENABLEMENT_QUERY_URL, capabilityName);
        stubEnableCapabilities(url, statusToReturn, 1);
    }

    public static void stubEnableAllCapabilities() {
        String url = format(CAPABILITY_ENABLEMENT_QUERY_URL, ".*");
        stubEnableCapabilities(url, true, 2);
    }

    private static void stubEnableCapabilities(String stubUrl, boolean statusToReturn, int priority) {
        String responsePayload = Json.createObjectBuilder().add("enabled", statusToReturn).build().toString();

        stubFor(get(urlMatching(stubUrl))
                .atPriority(priority)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withBody(responsePayload)));
    }
}
