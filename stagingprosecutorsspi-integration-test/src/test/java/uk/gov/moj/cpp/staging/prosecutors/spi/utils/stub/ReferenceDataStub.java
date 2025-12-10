package uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.readFile;

public class ReferenceDataStub {

    private static final String REFERENCE_DATA_PLEA_TYPES_MEDIA_TYPE = "application/vnd.referencedata.plea-types+json";
    private static final String REFERENCE_DATA_PLEA_TYPES_URL = "/referencedata-service/query/api/rest/referencedata/plea-types";


    public static void stubPleaTypeGuiltyFlags(){
        String payload = readFile("mockFiles/referencedata.query.plea-types.json");

        stubFor(get(urlPathMatching(REFERENCE_DATA_PLEA_TYPES_URL))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_PLEA_TYPES_MEDIA_TYPE)
                        .withBody(payload)));
    }

}
