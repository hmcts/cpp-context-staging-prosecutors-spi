package uk.gov.moj.cpp.staging.prosecutors.spi.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.http.HttpStatus.SC_OK;

import uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.SystemIdMapperStub;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public enum TestConfig {

    TEST_CONFIG_INSTANCE;

    public static final String CPP_SYSTEM_ID = "C00CommonPlatform";
    public static final String CJSE_SYSTEM_ID = "Z00CJSE";
    public static final String REFERENCEDATA_CJS_IT_SYSTEM_CODES_MEDIA_TYPE = "application/vnd.referencedata.cjs-it-system-codes+json";
    public static final String REFERENCEDATA_QUERY_URL_CJS_IT_SYSTEM_CODES = "/referencedata-service/query/api/rest/referencedata/cjs-it-system-codes";
    public static final String REFERENCEDATA_PTI_URN_TO_OU_CODE_MEDIA_TYPE = "application/vnd.referencedata.query.ptiurn-to-oucode+json";
    public static final String REFERENCEDATA_QUERY_PTIURN_TO_OUCODE = "/referencedata-service/query/api/rest/referencedata/ptiurn";

    private String urn;
    private String requestId;
    private String correlationId;
    private UUID resultId;
    private UUID caseId;
    private UUID oiId;


    public static void stubGetCjsItSystemCodes() {
        try {

            stubFor(get(urlPathEqualTo(REFERENCEDATA_QUERY_URL_CJS_IT_SYSTEM_CODES))
                    .willReturn(aResponse().withStatus(SC_OK)
                            .withHeader("CPPID", UUID.randomUUID().toString())
                            .withHeader("Content-Type", REFERENCEDATA_CJS_IT_SYSTEM_CODES_MEDIA_TYPE)
                            .withBody(readFileToString(new File(TestConfig.class.getClassLoader().getResource("mockFiles/cjs-it-system-codes.json").getFile())))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void stubGetOucodeForPtiUrn() {
        try {

            stubFor(get(urlPathEqualTo(REFERENCEDATA_QUERY_PTIURN_TO_OUCODE))
                    .willReturn(aResponse().withStatus(SC_OK)
                            .withHeader("CPPID", UUID.randomUUID().toString())
                            .withHeader("Content-Type", REFERENCEDATA_PTI_URN_TO_OU_CODE_MEDIA_TYPE)
                            .withBody(readFileToString(new File(TestConfig.class.getClassLoader().getResource("mockFiles/pti-urn-to-ou-code.json").getFile())))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUp() {

        this.urn = UrnGenerator.withNonAthenaPoliceForceCode().next();
        this.requestId = randomUUID().toString();
        this.correlationId = randomUUID().toString();
        this.resultId = randomUUID();
        this.caseId = randomUUID();
        this.oiId = randomUUID();
        SystemIdMapperStub.stubGetCaseIdByURN(urn, caseId);
        SystemIdMapperStub.stubGetRequestUUIDByMdiRequestId(requestId, randomUUID());
        SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId(correlationId.concat(CPP_SYSTEM_ID), oiId);
        SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId(correlationId.concat(CJSE_SYSTEM_ID), oiId);
        SystemIdMapperStub.stubGetResultIdByURN(urn, resultId);

    }


    public String getURN() {
        return urn;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getOiId() {
        return oiId;
    }
}
