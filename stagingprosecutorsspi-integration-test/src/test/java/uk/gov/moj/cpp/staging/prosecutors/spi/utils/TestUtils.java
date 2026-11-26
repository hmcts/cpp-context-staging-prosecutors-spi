package uk.gov.moj.cpp.staging.prosecutors.spi.utils;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.Boolean.FALSE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.QueryHelper.verifyAndReturnSpiOutMessage;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

public class TestUtils {

    public static final String CJSE_DELIVERY_URL = "/simulator/CJSE/message";
    protected static final int DEFAULT_DELAY_INTERVAL_IN_MILLIS = 5000;
    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);
    private static final String XSD_SPI_OUT_RESULT = "xsd/CPP-StdProsPoliceResult-v1-2.xsd";

    /**
     * Waits for the time identified by field {@value DEFAULT_DELAY_INTERVAL_IN_MILLIS}
     */
    public static void waitForTimeToElapse() {
        waitForTimeToElapse(DEFAULT_DELAY_INTERVAL_IN_MILLIS);
    }

    /**
     * Waits for the specified period of time
     */
    @SuppressWarnings("squid:S2925")
    public static void waitForTimeToElapse(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            LOGGER.error("Error waiting for thread delay", e);
        }
    }

    public static String readFile(String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (Exception e) {
            LOGGER.error("Error consuming file from location {}", ramlPath);
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

    public static String readFile(final String path, final Object... placeholders) {
        return String.format(readFile(path), placeholders);
    }

    public static void stubPCFcommand(final UUID userId) {
        stubFor(post(urlPathMatching("/prosecutioncasefile-service/command/api/rest/prosecutioncasefile.*"))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader(ID, userId.toString())
                        .withHeader(CONTENT_TYPE, "application/json")));

    }

    public static String verifyMdiMessage(final String lastPostedCommand, String expectedFile) {

        final String expectedSPIOutPayload = readFile(expectedFile);
        final Diff diff = DiffBuilder
                .compare(expectedSPIOutPayload)
                .withTest(lastPostedCommand)
                .withNodeFilter(node -> !node.getNodeName().equals("RequestID")
                        && !node.getNodeName().equals("Timestamp")
                        && !node.getNodeName().equals("Message"))
                .build();
        assertThat(diff.hasDifferences(), is(FALSE));
        return lastPostedCommand;
    }

    public static void verifyOIMessage(final String lastPostedCommand, String expectedFile) {
        final Diff diff;
        final String message = StringEscapeUtils.unescapeXml(extractOIMessageFromPayload(lastPostedCommand));
        final String expectedSpiOutOiPayload = readFile(expectedFile);
        diff = DiffBuilder
                .compare(expectedSpiOutOiPayload)
                .withTest(message)
                .withNodeFilter(node -> !node.getNodeName().equals("CorrelationID")
                        && !node.getNodeName().equals("ns2:SystemDataStreamID")
                        && !node.getNodeName().equals("ns2:DataStreamContent")
                )
                .build();

        assertThat(diff.hasDifferences(), is(FALSE));
    }

    public static void verifyResultedCaseMessage(final String lastPostedCommand, String expectedFile) throws IOException, SAXException {

        XMLUnit.setNormalizeWhitespace(true);
        final Diff diff;
        final String payload = StringEscapeUtils.unescapeXml(StringEscapeUtils.unescapeXml(extractOIMessageFromPayload(lastPostedCommand)));
        final String resultedCaseMessage = "<ResultedCaseMessage" + StringUtils.substringBetween(payload, "<ResultedCaseMessage", "</ResultedCaseMessage>") + "</ResultedCaseMessage>";
        final String expectedResultedCaseMessage = readFile(expectedFile);

        diff = DiffBuilder
                .compare(expectedResultedCaseMessage)
                .normalizeWhitespace()
                .withTest(resultedCaseMessage)
                .withNodeFilter(node -> !node.getNodeName().equals("PTIURN") && !node.getNodeName().equals("ProsecutorReference"))
                .build();

        if (diff.hasDifferences()) {
            LOGGER.error(diff.toString());
        }

        assertThat(diff.hasDifferences(), is(FALSE));

        XmlSchemaValidator.validate(resultedCaseMessage, XSD_SPI_OUT_RESULT);

    }

    public static void verifyResultedCaseMessageUsingQuery(final String caseUrn, final String defendantProsecutorReference, final String expectedValue, final String expectedXmlPayload) throws IOException, SAXException {

        final String payloadFromQuery = verifyAndReturnSpiOutMessage(caseUrn, defendantProsecutorReference, expectedValue);

        XMLUnit.setNormalizeWhitespace(true);

        final Diff diff = DiffBuilder
                .compare(expectedXmlPayload)
                .normalizeWhitespace()
                .withTest(createReader(new StringReader(payloadFromQuery)).readObject().getString("payload"))
                .withNodeFilter(node -> !node.getNodeName().equals("PTIURN") && !node.getNodeName().equals("ProsecutorReference"))
                .build();

        if (diff.hasDifferences()) {
            LOGGER.error(diff.toString());
        }

        assertThat(diff.hasDifferences(), is(FALSE));
    }

    public static String getLastPostedCommand() {
        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL)));
        if (!loggedRequests.isEmpty()) {
            return loggedRequests.get(loggedRequests.size() - 1).getBodyAsString();
        }
        return null;
    }


    public static String getLastPostedCommand(String caseUrn) {
        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL)));

        await().timeout(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL))
                                .withRequestBody(containing(caseUrn))

                        ).size(), is(1));

        if (!loggedRequests.isEmpty()) {
            return loggedRequests.get(loggedRequests.size() - 1).getBodyAsString();
        }
        return null;
    }

    public static String extractOIMessageFromPayload(final String payload) {
        final String startTag = "<Message>";
        final String endTag = "</Message>";
        final int startTagIndex = payload.indexOf(startTag);
        final int endTagIndex = payload.indexOf(endTag);

        return payload.substring(startTagIndex + startTag.length(), endTagIndex);
    }

    public static String extractValueFromXmlString(final String xml, final String fieldName) {
        final String start = String.format("%s", fieldName);
        final String end = String.format("/%s", fieldName);
        final int startIndex = xml.indexOf(start);
        final int endIndex = xml.indexOf(end);

        if (startIndex < 0 || endIndex < 0) {
            return null;
        }
        return xml.substring(startIndex + start.length(), endIndex);
    }
}
