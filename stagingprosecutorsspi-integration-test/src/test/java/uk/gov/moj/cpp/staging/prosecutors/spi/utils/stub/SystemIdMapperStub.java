package uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.readFile;

import java.util.UUID;

public class SystemIdMapperStub {


    public static void stubGetCaseIdByURN(final String urn, final UUID cppCaseId) {
        String systemMapping = readFile("mockFiles/systemid.mapping.json");
        systemMapping = systemMapping.replace("CASE-UUID", cppCaseId.toString());

        stubFor(get(urlPathMatching("/system-id-mapper-api/rest/systemid/mappings"))
                .withQueryParam("sourceId", containing(urn))
                .withQueryParam("sourceType", containing("OU_URN"))
                .withQueryParam("targetType", containing("CASE_FILE_ID"))
                .willReturn(aResponse().withStatus(200)
                        .withBody(systemMapping)));
    }

    public static void stubGetResultIdByURN(final String urn, final UUID resultId) {
        String systemMapping = readFile("mockFiles/systemid.mapping.resultId.json");
        systemMapping = systemMapping.replace("RESULT-UUID", resultId.toString());

        stubFor(get(urlPathMatching("/system-id-mapper-api/rest/systemid/mappings"))
                .withQueryParam("sourceId", containing(urn))
                .withQueryParam("sourceType", containing("SPI-URN"))
                .withQueryParam("targetType", containing("SPI-RESULT-ID"))
                .willReturn(aResponse().withStatus(200)
                        .withBody(systemMapping)));
    }

    public static void stubGetRequestUUIDByMdiRequestId(final String mdiInputRequestId, final UUID outputRequestId) {
        String systemMapping = readFile("mockFiles/systemid.mapping.requestId.json");
        systemMapping = systemMapping.replace("REQUEST-UUID", outputRequestId.toString());

        stubFor(get(urlPathMatching("/system-id-mapper-api/rest/systemid/mappings"))
                .withQueryParam("sourceId", containing(mdiInputRequestId))
                .withQueryParam("sourceType", containing("REQUEST-MDI-ID"))
                .withQueryParam("targetType", containing("REQUEST-ID"))
                .willReturn(aResponse().withStatus(200)
                        .withBody(systemMapping)));
    }

    public static void stubGetRequestUUIDBySystemId(final String systemId, final UUID idOfOperationalInterface) {
        String systemMapping = readFile("mockFiles/systemid.mapping.Oiid.json");
        systemMapping = systemMapping.replace("OI-UUID", idOfOperationalInterface.toString());

        stubFor(get(urlPathMatching("/system-id-mapper-api/rest/systemid/mappings"))
                .withQueryParam("sourceId", containing(systemId))
                .withQueryParam("sourceType", containing("COORRELATION_SYSTEM_ID"))
                .withQueryParam("targetType", containing("REQUEST-OI-ID"))
                .willReturn(aResponse().withStatus(200)
                        .withBody(systemMapping)));
    }

    public static void stubGetRequestUUIDByCorrelationIdAndSystemId(final String correlationIdAndSystemId, final UUID idOfOperationalInterface) {
        String systemMapping = readFile("mockFiles/systemid.mapping.Oiid.json");
        systemMapping = systemMapping.replace("OI-UUID", idOfOperationalInterface.toString());

        stubFor(get(urlPathMatching("/system-id-mapper-api/rest/systemid/mappings"))
                .withQueryParam("sourceId", containing(correlationIdAndSystemId))
                .withQueryParam("sourceType", containing("COORRELATION_SYSTEM_ID"))
                .withQueryParam("targetType", containing("REQUEST-OI-ID"))
                .willReturn(aResponse().withStatus(200)
                        .withBody(systemMapping)));
    }

}
