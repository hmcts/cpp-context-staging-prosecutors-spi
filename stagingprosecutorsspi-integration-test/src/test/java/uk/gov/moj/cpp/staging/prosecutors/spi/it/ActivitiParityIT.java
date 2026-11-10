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
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllScenarios;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
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
 * The J25 upgrade replaces it with the platform activiti-embedded-rest library, which deploys processes/*.bpmn20.xml by
 * file name. Two changes are accepted and pinned as J25 behaviour: that deployment (SPI-M04) and the Activiti REST API
 * the library publishes (SPI-M03, its 3 Activiti resources only). Every other assertion encodes the J17 result; history
 * level NONE (SPI-M02) needs cpp.activiti.historyLevel=none in the event processor's application.properties.
 *
 * The SPI-M01 failure path (a timer job that fails after its JMS send) is in ActivitiTimerJobRollbackParityIT.
 */
class ActivitiParityIT extends AbstractIT {

    private static final String VIEWSTORE_JDBC_URL = "jdbc:postgresql://" + HOST + ":5432/stagingprosecutorsspiviewstore";
    private static final String DB_USER_AND_PASSWORD = "stagingprosecutorsspi";
    private static final String ACTIVITI_REST_URL = BASE_URI + "/stagingprosecutorsspi-service/internal/activiti/service";
    private static final String EMPTY_LIST = "{\"data\":[],\"total\":0}";

    private static final String TIMER_FLOW_JOBS = "from act_ru_job where proc_def_id_ like 'timer_flow:%'";
    private static final String TIMER_FLOW_EXECUTIONS = "from act_ru_execution where proc_def_id_ like 'timer_flow:%'";
    private static final String TIMER_FLOW_HISTORIC_PROCESS_INSTANCES = "from act_hi_procinst where proc_def_id_ like 'timer_flow:%'";
    private static final String TIMER_FLOW_HISTORIC_ACTIVITIES = "from act_hi_actinst where proc_def_id_ like 'timer_flow:%'";
    private static final String TIMER_FLOW_HISTORIC_VARIABLES = "from act_hi_varinst v join act_hi_procinst p on p.id_ = v.proc_inst_id_ where p.proc_def_id_ like 'timer_flow:%'";

    @BeforeEach
    void setUp() throws Exception {
        TEST_CONFIG_INSTANCE.setUp();
        resetAllRequests();
        // The CJSE stub is a WireMock scenario (first POST fails, later ones succeed). Without a reset, the second
        // retry test starts in "Cause Success": its first POST succeeds and no retry is scheduled.
        resetAllScenarios();
        SystemIdMapperStub.stubGetRequestUUIDBySystemId(CPP_SYSTEM_ID, randomUUID());
    }

    // SPI-M01 (BC-09), normal flow: on J17 the timer job's DB work and its JMS send share one JTA transaction, so the
    // retry is sent once and the timer_flow job is removed.
    @Test
    void spiM01_retryShouldBeSentExactlyOnceAndLeaveNoTimerFlowJob() throws Exception {
        triggerOneCjseRetry();

        awaitFirstSubmissionAndExactlyOneRetry();

        await().pollInterval(1, SECONDS).atMost(30, SECONDS)
                .until(() -> count("select count(*) " + TIMER_FLOW_JOBS), is(0L));
    }

    // SPI-M02: J17 runs with history level NONE, so a retry adds no history rows. The rows are counted for timer_flow
    // only, before and after this test's retry, so other ITs that ran against the same database do not change the result.
    @Test
    void spiM02_retryShouldAddNoActivitiHistory() throws Exception {
        final long historicProcessInstancesBefore = count("select count(*) " + TIMER_FLOW_HISTORIC_PROCESS_INSTANCES);
        final long historicActivitiesBefore = count("select count(*) " + TIMER_FLOW_HISTORIC_ACTIVITIES);
        final long historicVariablesBefore = count("select count(*) " + TIMER_FLOW_HISTORIC_VARIABLES);
        final long executionsBefore = count("select count(*) " + TIMER_FLOW_EXECUTIONS);

        triggerOneCjseRetry();

        awaitFirstSubmissionAndExactlyOneRetry();
        // Wait for this retry's process to end. Executions that other tests left behind are in the baseline.
        await().pollInterval(1, SECONDS).atMost(30, SECONDS)
                .until(() -> count("select count(*) " + TIMER_FLOW_EXECUTIONS), lessThanOrEqualTo(executionsBefore));

        final long addedHistoricProcessInstances = count("select count(*) " + TIMER_FLOW_HISTORIC_PROCESS_INSTANCES) - historicProcessInstancesBefore;
        final long addedHistoricActivities = count("select count(*) " + TIMER_FLOW_HISTORIC_ACTIVITIES) - historicActivitiesBefore;
        final long addedHistoricVariables = count("select count(*) " + TIMER_FLOW_HISTORIC_VARIABLES) - historicVariablesBefore;
        final String observed = format("added by one retry: act_hi_procinst=%d, act_hi_actinst=%d, act_hi_varinst=%d; historic variable names=%s",
                addedHistoricProcessInstances, addedHistoricActivities, addedHistoricVariables,
                strings("select distinct v.name_ " + TIMER_FLOW_HISTORIC_VARIABLES + " order by v.name_"));

        assertThat(observed, addedHistoricProcessInstances, is(0L));
        assertThat(observed, addedHistoricActivities, is(0L));
        assertThat(observed, addedHistoricVariables, is(0L));
    }

    // SPI-M03, accepted J25 behaviour (team decision, SPI PR #48): J17 SPI ran its own engine and had no Activiti REST
    // API; on J25 the platform activiti-embedded-rest library publishes one under /internal/activiti/service. Its only
    // callers are the sjp and prosecution-casefile IT ActivitiHelpers (and cpp-apitests, against sjp), which list
    // process instances, historic process instances and timer jobs, then POST {"action":"execute"} to a job. The
    // queries are pinned here with random ids, so they match nothing; the POST runs the job at once and is not exercised.
    @Test
    void spiM03_activitiRuntimeApiShouldBeExposed() throws IOException, InterruptedException {
        assertAnswered(get(ACTIVITI_REST_URL + "/runtime/process-instances?processDefinitionKey=timer_flow&businessKey=" + randomUUID()),
                200, EMPTY_LIST);
    }

    @Test
    void spiM03_activitiHistoryApiShouldBeExposed() throws IOException, InterruptedException {
        assertAnswered(get(ACTIVITI_REST_URL + "/history/historic-process-instances?processDefinitionKey=timer_flow&businessKey=" + randomUUID()),
                200, EMPTY_LIST);
    }

    @Test
    void spiM03_activitiJobQueryApiShouldBeExposed() throws IOException, InterruptedException {
        assertAnswered(get(ACTIVITI_REST_URL + "/management/jobs?timersOnly=true&processInstanceId=" + randomUUID()),
                200, EMPTY_LIST);
    }

    // SPI-M03: only the Activiti resources are published under /internal/activiti/service. An empty JAX-RS Application
    // gets every resource in the WAR, which republished SPI's command and query endpoints, the event-store event pages
    // and the framework stream endpoints there. OPTIONS answers for any matched resource, so a 404 means "not published".
    @Test
    void spiM03_spiAndFrameworkEndpointsShouldNotBeRepublishedUnderTheActivitiPath() throws IOException, InterruptedException {
        for (final String path : List.of("/spi", "/cppmessages/" + randomUUID(), "/cpp-message/" + randomUUID(),
                "/event-streams/1/BACKWARD/10", "/streams", "/stream-errors")) {
            final HttpResponse<String> response = options(ACTIVITI_REST_URL + path);
            assertThat(observed(response), response.statusCode(), is(404));
        }
    }

    // SPI-M04: on J17 activiti-cdi deployed timer_flow from processes/time_delay.bpmn (via processes.xml). The J25
    // library deploys processes/*.bpmn20.xml and names each resource by its file name; the BPMN content is unchanged.
    // The latest version is checked, because the first J25 start on a J17 database adds a version next to the J17 ones.
    @Test
    void spiM04_timerFlowShouldBeDeployedFromTimeDelayBpmn() {
        final List<String> resourceNames = resourceNamesOfTimerFlowDefinitionsNewestFirst();

        assertThat("timer_flow resources, newest version first: " + resourceNames, resourceNames.get(0), is("time_delay.bpmn20.xml"));
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

    private void awaitFirstSubmissionAndExactlyOneRetry() {
        await().pollDelay(2, SECONDS).pollInterval(1, SECONDS).atMost(15, SECONDS)
                .until(this::cjsePostsForThisUrn, is(2));
        await().during(10, SECONDS).atMost(15, SECONDS)
                .until(this::cjsePostsForThisUrn, is(2));
    }

    private int cjsePostsForThisUrn() {
        return findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL)).withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size();
    }

    private static HttpResponse<String> get(final String url) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> options(final String url) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void assertAnswered(final HttpResponse<String> response, final int status, final String body) {
        assertThat(observed(response), response.statusCode(), is(status));
        assertThat(observed(response), response.headers().firstValue("Content-Type").orElse(""), startsWith("application/json"));
        assertThat(observed(response), response.body(), is(body));
    }

    private static String observed(final HttpResponse<String> response) {
        return format("%s %s answered %d: %s", response.request().method(), response.request().uri(), response.statusCode(), response.body());
    }

    private static long count(final String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(VIEWSTORE_JDBC_URL, DB_USER_AND_PASSWORD, DB_USER_AND_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static List<String> strings(final String sql) {
        try (Connection connection = DriverManager.getConnection(VIEWSTORE_JDBC_URL, DB_USER_AND_PASSWORD, DB_USER_AND_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            final List<String> values = new ArrayList<>();
            while (resultSet.next()) {
                values.add(resultSet.getString(1));
            }
            return values;
        } catch (final SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> resourceNamesOfTimerFlowDefinitionsNewestFirst() {
        return strings("select resource_name_ from act_re_procdef where key_ = 'timer_flow' order by version_ desc");
    }
}
