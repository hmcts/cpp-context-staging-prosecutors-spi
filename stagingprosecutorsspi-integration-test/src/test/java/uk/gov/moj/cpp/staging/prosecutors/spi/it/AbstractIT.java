package uk.gov.moj.cpp.staging.prosecutors.spi.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.stubGetCjsItSystemCodes;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.stubGetOucodeForPtiUrn;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.WiremockTestHelper.resetService;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.CJSESoapServiceStub.stubCJSESoapService;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.SystemIdMapperStub.stubGetRequestUUIDByCorrelationIdAndSystemId;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.staging.prosecutors.spi.helper.QueryHelper;
import uk.gov.moj.cpp.staging.prosecutors.spi.utils.SPISoapAdapterHelper;

import java.util.Objects;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public abstract class AbstractIT {

    @BeforeAll
    public static void initialSetup() {
        resetService();
        stubAccessControl(true, fromString(QueryHelper.SYSTEM_USER_ID), "System Users");
        stubAccessControl(true, fromString(SPISoapAdapterHelper.CJSE_USER_ID), "CJSE");
        stubAccessControl(true, null, "CJSE", "System Users");
        stubGetRequestUUIDByCorrelationIdAndSystemId("INVALID_CORRELATION_ID".concat("INVALID_SYSTEM_ID"), randomUUID());
        stubCJSESoapService();
        stubGetCjsItSystemCodes();
        stubGetOucodeForPtiUrn();

    }

    private static void stubAccessControl(final boolean grantAccess, final UUID userId, final String... groupNames) {

        final JsonArrayBuilder groupsArray = createArrayBuilder();

        if (grantAccess) {
            for (final String groupName : groupNames) {
                groupsArray.add(createObjectBuilder()
                        .add("groupId", randomUUID().toString())
                        .add("groupName", groupName)
                );
            }
        }

        final JsonObject response = createObjectBuilder()
                .add("groups", groupsArray).build();

        final String urlPath = Objects.nonNull(userId) ? "/usersgroups-service/query/api/rest/usersgroups/users/" + userId + "/groups" : "/usersgroups-service/query/api/rest/usersgroups/users/[^/]*/groups";
        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, Objects.nonNull(userId) ? userId.toString() : randomUUID().toString())
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(response.toString())));

    }
}
