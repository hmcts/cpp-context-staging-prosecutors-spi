package uk.gov.moj.cpp.staging.prosecutors.spi.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.WiremockTestHelper.BASE_URI;

import uk.gov.justice.services.common.http.HeaderConstants;

import com.google.common.base.Joiner;

public class QueryHelper {

    public static final String SYSTEM_USER_ID = randomUUID().toString();

    private static final String READ_BASE_URL = "/stagingprosecutorsspi-query-api/query/api/rest/stagingprosecutors-spi";


    public static String getReadUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }

    public static String verifyAndReturnSpiOutMessage(final String caseUrn, final String defendantProsecutorReference, final String expectedValue) {

        return poll(requestParams(getReadUrl(String.format("/spi-out-message?caseUrn=%s&defendantProsecutorReference=%s", caseUrn, defendantProsecutorReference)),
                "application/vnd.stagingprosecutorsspi.query.spi-out-message+json")
                .withHeader(HeaderConstants.USER_ID, SYSTEM_USER_ID))
                .timeout(60, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.payload", containsString(expectedValue))
                        ))).getPayload();
    }

    public static void verifySpiMessageNotFound(final String caseUrn, final String defendantProsecutorReference) {

        poll(requestParams(getReadUrl(String.format("/spi-out-message?caseUrn=%s&defendantProsecutorReference=%s", caseUrn, defendantProsecutorReference)),
                "application/vnd.stagingprosecutorsspi.query.spi-out-message+json")
                .withHeader(HeaderConstants.USER_ID, SYSTEM_USER_ID))
                .timeout(60, SECONDS)
                .until(
                        status().is(NOT_FOUND)
                );
    }

}
