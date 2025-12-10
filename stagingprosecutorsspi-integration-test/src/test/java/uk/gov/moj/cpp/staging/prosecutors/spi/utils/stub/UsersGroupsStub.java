package uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.readFile;

public class UsersGroupsStub {

    public static void stubGetUsersDetails() {
        final String userDetails = readFile("mockFiles/usersgroups.query.user.json");

        stubFor(get(urlPathMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*"))
                .willReturn(aResponse().withStatus(200)
                        .withBody(userDetails)));
    }
}
