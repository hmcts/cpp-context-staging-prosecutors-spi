package uk.gov.moj.cpp.staging.prosecutors.spi.it;

import uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.SystemIdMapperStub;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.JMSTopicHelper.postMessageToTopicAndVerify;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS;
import static uk.gov.moj.cpp.staging.prosecutors.spi.helper.SPIInSoapHelper.sendAndVerifySPImessage;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.CPP_SYSTEM_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestConfig.TEST_CONFIG_INSTANCE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.CJSE_DELIVERY_URL;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.readFile;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.TestUtils.stubPCFcommand;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.WiremockTestHelper.BASE_URI;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.WiremockTestHelper.HOST;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.CJSESoapServiceStubForRetry.stubCJSESoapService;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.ProsecutionCaseFileStub.stubGetProsecutionCaseFile;

/**
 * J17/J25 parity characterisation (J17 golden master) for the Activiti engine that runs the SPI result retry timer.
 *
 * On J17 the event processor builds its own engine (ProgrammaticProcessEngineConfiguration): JTA transactions,
 * history level NONE, process deployed by activiti-cdi from processes/time_delay.bpmn, and no Activiti REST API.
 * The J25 upgrade replaces it with the activiti-embedded-rest library. Every assertion below encodes the J17 result.
 */
class ActivitiParityIT extends AbstractIT {

    private static final String VIEWSTORE_JDBC_URL = "jdbc:postgresql://" + HOST + ":5432/stagingprosecutorsspiviewstore";
    private static final String DB_USER_AND_PASSWORD = "stagingprosecutorsspi";
    private static final String ACTIVITI_REST_PROCESS_INSTANCES_URL = BASE_URI + "/stagingprosecutorsspi-service/internal/activiti/service/runtime/process-instances";

    @BeforeEach
    void setUp() throws Exception {
        TEST_CONFIG_INSTANCE.setUp();
        resetAllRequests();
        SystemIdMapperStub.stubGetRequestUUIDBySystemId(CPP_SYSTEM_ID, randomUUID());
    }

    // SPI-M01 (BC-09): on J17 the timer job's DB work and its JMS send share one JTA transaction, so the retry is
    // sent once and the job is removed. SPI-M02: J17 runs with history level NONE, so the retry leaves no history rows.
    @Test
    void spiM01_spiM02_retryShouldBeSentExactlyOnceAndLeaveNoActivitiJobsOrHistory() throws Exception {
        triggerOneCjseRetry();

        await().pollDelay(2, SECONDS).pollInterval(1, SECONDS).atMost(15, SECONDS)
                .until(this::cjsePostsForThisUrn, is(2));
        await().during(10, SECONDS).atMost(15, SECONDS)
                .until(this::cjsePostsForThisUrn, is(2));

        await().pollInterval(1, SECONDS).atMost(30, SECONDS)
                .until(() -> count("select count(*) from act_ru_job"), is(0L));
        assertThat(count("select count(*) from act_hi_procinst"), is(0L));
        assertThat(count("select count(*) from act_hi_actinst"), is(0L));
        assertThat(count("select count(*) from act_hi_varinst"), is(0L));
    }

    // SPI-M03 (BC-10): J17 exposes no Activiti REST API; J25 adds /internal/activiti/service.
    @Test
    void spiM03_activitiRestApiShouldNotBeExposed() throws IOException, InterruptedException {
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(ACTIVITI_REST_PROCESS_INSTANCES_URL)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(404));
    }

    // SPI-M04: on J17 activiti-cdi deploys timer_flow from processes/time_delay.bpmn (via processes.xml).
    @Test
    void spiM04_timerFlowShouldBeDeployedFromTimeDelayBpmn() {
        assertThat(resourceNamesOfTimerFlowDefinitions(), contains("processes/time_delay.bpmn"));
    }

    private void triggerOneCjseRetry() throws Exception {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        final String policeResultGenerated = readFileToString(new File(getClass().getClassLoader().getResource("external/message/public.results.police-result-generated.json").getFile()))
                .replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        stubCJSESoapService();
        postMessageToTopicAndVerify(policeResultGenerated, "public.results.police-result-generated", "stagingprosecutorsspi.event.cppmessage-retry-delay-required");
    }

    private int cjsePostsForThisUrn() {
        return findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL)).withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size();
    }

    private static long count(final String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(VIEWSTORE_JDBC_URL, DB_USER_AND_PASSWORD, DB_USER_AND_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static List<String> resourceNamesOfTimerFlowDefinitions() {
        try (Connection connection = DriverManager.getConnection(VIEWSTORE_JDBC_URL, DB_USER_AND_PASSWORD, DB_USER_AND_PASSWORD);
             PreparedStatement statement = connection.prepareStatement("select resource_name_ from act_re_procdef where key_ = 'timer_flow'");
             ResultSet resultSet = statement.executeQuery()) {
            final List<String> resourceNames = new ArrayList<>();
            while (resultSet.next()) {
                resourceNames.add(resultSet.getString(1));
            }
            return resourceNames;
        } catch (final SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
