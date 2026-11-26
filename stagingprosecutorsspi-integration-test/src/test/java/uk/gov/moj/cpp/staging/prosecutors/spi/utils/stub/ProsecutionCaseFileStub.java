package uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class ProsecutionCaseFileStub {

    public static void stubGetProsecutionCaseFile(final String caseFile) {
        stubFor(get(urlPathMatching("/prosecutioncasefile-service/query/api/rest/prosecutioncasefile/cases/.*"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(caseFile)));
    }
}
