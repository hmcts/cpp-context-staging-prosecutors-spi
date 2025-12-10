package uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CJSESoapServiceStubForRetry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CJSESoapServiceStubForRetry.class);
    private static final String CJSE_DELIVERY_URL = "/simulator/CJSE/message";

    public static void stubCJSESoapService() {
        String responseXml;
        String responseXml_Invalid_ResponseCode;
        try {
            responseXml = Resources.toString(Resources.getResource("mockFiles/xmlResponseExample.xml"),
                    Charset.defaultCharset());
            responseXml_Invalid_ResponseCode = Resources.toString(Resources.getResource("mockFiles/xmlResponseExample-invalidResponse.xml"),
                    Charset.defaultCharset());
        } catch (IOException e) {
            LOGGER.error("Error getting resource for stubbing " + e);
            throw new RuntimeException(e);
        }

        LOGGER.info("Stubbed Ping for CJSE");

        stubFor(post(urlMatching(CJSE_DELIVERY_URL))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(responseXml_Invalid_ResponseCode))
                .willSetStateTo("Cause Success"));

        stubFor(post(urlMatching(CJSE_DELIVERY_URL))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Cause Success")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(responseXml)));

        stubFor(get(urlMatching(CJSE_DELIVERY_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(responseXml)));

    }
}
