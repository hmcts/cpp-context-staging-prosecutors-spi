package uk.gov.moj.cpp.staging.prosecutors.spi.it;

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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.WiremockTestHelper.HOST;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.CJSESoapServiceStubForRetry.stubCJSESoapService;
import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.ProsecutionCaseFileStub.stubGetProsecutionCaseFile;

import uk.gov.moj.cpp.staging.prosecutors.spi.utils.stub.SystemIdMapperStub;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * J17 -> J25 parity (golden master), finding SPI-M01 / BC-09: a failure while Activiti writes the CJSE retry timer.
 *
 * The CJSE retry timer (timer_flow, time_delay BPMN) runs ProcessTimerFinished as an Activiti timer job: the delegate
 * sends stagingprosecutorsspi.command.handler.receive-retry-delay-timer-finished over JMS, then the job commits its own
 * Activiti state. On Java 17 the context's ProgrammaticProcessEngineConfiguration (CdiJtaProcessEngineConfiguration)
 * runs the job in one JTA transaction, so a failure while the job commits rolls back every row and the JMS send, and
 * Activiti's job retry sends the command once more. On Java 25 the activiti-embedded-rest library runs Activiti with a
 * no-op transaction manager and transactionsExternallyManaged=true: each statement commits on its own and the JMS send
 * is not rolled back.
 *
 * Activiti 5.22 (both sides) flushes inserts, then updates, then deletes in EntityDependencyOrder.DELETE_ORDER: the
 * timer job row first, then the process variables, then the execution. Inserts go in the reverse order, so the timer
 * job row is the last runtime row a process start writes. Each test fails one of those writes once:
 * - the timer job delete: nothing the retry depends on is gone, so Activiti runs the job again. On J25 the first send
 *   is already out, the retry sends again, and CPPMessage.sendMessageForRetry has no guard against a second
 *   timer-finished command after a recorded success. One test counts the CJSE submissions, the other reads the
 *   CPPMessage stream in the event store, which records every send as cppmessage-prepared-for-sending.
 * - a variable delete: the timer job row is already deleted by then. On J25 that delete stays committed, so the job is
 *   never retried and the execution and its variables are left behind.
 * - the timer job insert while CPPMessagePreparedProcessor starts the timer: the event processor's JTA transaction
 *   rolls back and the retry-delay-required event is redelivered. The process start must leave nothing behind from
 *   the failed attempt, or an execution without a timer stays in the runtime tables for good.
 *
 * The one-shot flag is a sequence because nextval() survives a transaction rollback. The trigger exists only while a
 * test runs.
 *
 * Golden (Java 17): the failed attempt leaves no trace, so CJSE receives the first submission plus exactly one retry,
 * the CPPMessage stream records one retry, and the timer process completes.
 */
class ActivitiTimerJobRollbackParityIT extends AbstractIT {

    private static final String VIEWSTORE_JDBC_URL = "jdbc:postgresql://" + HOST + ":5432/stagingprosecutorsspiviewstore";
    private static final String EVENTSTORE_JDBC_URL = "jdbc:postgresql://" + HOST + ":5432/stagingprosecutorsspieventstore";
    private static final String DB_USER_AND_PASSWORD = "stagingprosecutorsspi";

    private static final String FAULT_SEQUENCE = "spi_m01_fault_once";
    private static final String FAULT_FUNCTION = "spi_m01_fail_first_timer_flow_write";
    private static final String FAULT_TRIGGER = "spi_m01_fail_first_timer_flow_write";

    private static final String TIMER_FLOW_JOBS = "from act_ru_job where proc_def_id_ like 'timer_flow:%'";
    private static final String TIMER_FLOW_EXECUTIONS = "from act_ru_execution where proc_def_id_ like 'timer_flow:%'";
    private static final String UNFINISHED_TIMER_FLOW_HISTORY = "from act_hi_procinst where proc_def_id_ like 'timer_flow:%' and end_time_ is null";

    // act_ru_job carries the process definition; act_ru_variable is matched through its process instance. A new job row
    // is matched through its process instance too: the execution is inserted first, in the same transaction.
    private static final String TIMER_FLOW_JOB_ROW = "old.proc_def_id_ like 'timer_flow:%'";
    private static final String TIMER_FLOW_VARIABLE_ROW = "exists (select 1 from act_ru_execution e where e.id_ = old.proc_inst_id_ and e.proc_def_id_ like 'timer_flow:%')";
    private static final String NEW_TIMER_FLOW_JOB_ROW = "exists (select 1 from act_ru_execution e where e.id_ = new.process_instance_id_ and e.proc_def_id_ like 'timer_flow:%')";

    // the CPPMessage stream on Java 17 after one failed CJSE submission and one successful retry
    private static final String[] CPP_MESSAGE_STREAM_WITH_ONE_RETRY = {
            "cppmessage-prepared-for-sending",
            "unsuccessful-sync-response-recorded",
            "cppmessage-retry-delay-required",
            "cppmessage-prepared-for-sending",
            "cpp-message-sent-succeessfully-recorded"};

    private long executionsBefore;
    private long unfinishedHistoryBefore;

    @BeforeEach
    void setUp() throws Exception {
        TEST_CONFIG_INSTANCE.setUp();
        resetAllRequests();
        resetAllScenarios();
        SystemIdMapperStub.stubGetRequestUUIDBySystemId(CPP_SYSTEM_ID, randomUUID());

        // no other retry timer may still be running, or it could consume the one-shot fault
        await().pollInterval(1, SECONDS).atMost(60, SECONDS)
                .until(() -> count("select count(*) " + TIMER_FLOW_JOBS + " and retries_ > 0"), is(0L));
        removeFaultInjection();
        // executions and history that earlier tests left behind are not this test's result
        executionsBefore = count("select count(*) " + TIMER_FLOW_EXECUTIONS);
        unfinishedHistoryBefore = count("select count(*) " + UNFINISHED_TIMER_FLOW_HISTORY);
    }

    @AfterEach
    void tearDown() throws SQLException {
        removeFaultInjection();
    }

    @Test
    void spiM01_failedTimerJobDeleteShouldNotSendTheCjseRetryTwice() throws Exception {
        installFaultInjection("delete", "act_ru_job", TIMER_FLOW_JOB_ROW);

        runOneCjseRetryThroughTheFault();

        assertOneCjseRetryAndNoTimerLeft();
    }

    @Test
    void spiM01_failedTimerJobDeleteShouldRecordTheCjseRetryOnceInTheCppMessageStream() throws Exception {
        installFaultInjection("delete", "act_ru_job", TIMER_FLOW_JOB_ROW);

        runOneCjseRetryThroughTheFault();

        final List<String> events = cppMessageStream();
        assertThat(format("observed: CPPMessage stream=%s, CJSE POSTs for this URN=%d", events, cjsePostsForThisUrn()),
                events, contains(CPP_MESSAGE_STREAM_WITH_ONE_RETRY));
    }

    @Test
    void spiM01_failedVariableDeleteShouldLeaveNoOrphanTimerFlowExecution() throws Exception {
        installFaultInjection("delete", "act_ru_variable", TIMER_FLOW_VARIABLE_ROW);

        runOneCjseRetryThroughTheFault();

        assertOneCjseRetryAndNoTimerLeft();
    }

    @Test
    void spiM01_failedTimerStartShouldLeaveNoPartialTimerFlowProcess() throws Exception {
        installFaultInjection("insert", "act_ru_job", NEW_TIMER_FLOW_JOB_ROW);

        runOneCjseRetryThroughTheFault();

        assertOneCjseRetryAndNoTimerLeft();
    }

    private void runOneCjseRetryThroughTheFault() throws Exception {
        triggerOneCjseRetry();

        await().pollInterval(1, SECONDS).atMost(60, SECONDS)
                .until(this::faultedWrites, greaterThanOrEqualTo(1L));
        // A failed job is retried after the next timer acquisition, and a failed timer start after the event's JMS
        // redelivery plus the timer delay. This does not fail on a timeout: the assertions report what happened.
        final long deadline = System.nanoTime() + SECONDS.toNanos(120);
        while (cjsePostsForThisUrn() < 2 && System.nanoTime() < deadline) {
            SECONDS.sleep(1);
        }
        await().pollInterval(1, SECONDS).atMost(120, SECONDS)
                .until(() -> count("select count(*) " + TIMER_FLOW_JOBS + " and retries_ > 0"), is(0L));
        // let any late CJSE submission arrive before counting
        SECONDS.sleep(15);
    }

    private void assertOneCjseRetryAndNoTimerLeft() throws Exception {
        final long executionsAdded = count("select count(*) " + TIMER_FLOW_EXECUTIONS) - executionsBefore;
        final long unfinishedHistoryAdded = count("select count(*) " + UNFINISHED_TIMER_FLOW_HISTORY) - unfinishedHistoryBefore;
        final String observed = format("observed: CJSE POSTs for this URN=%d, timer_flow jobs left=%s, timer_flow executions added=%d, unfinished timer_flow history added=%d, writes seen by the fault trigger=%d",
                cjsePostsForThisUrn(), timerFlowJobs(), executionsAdded, unfinishedHistoryAdded, faultedWrites());

        assertThat("first submission + exactly one retry; " + observed, cjsePostsForThisUrn(), is(2));
        assertThat("timer process completed; " + observed, timerFlowJobs(), is(empty()));
        assertThat("timer process completed; " + observed, executionsAdded, is(0L));
        assertThat("timer process completed; " + observed, unfinishedHistoryAdded, is(0L));
    }

    private void triggerOneCjseRetry() throws Exception {
        stubPCFcommand(randomUUID());
        stubGetProsecutionCaseFile(readFile("mockFiles/prosecutioncasefile.json"));
        sendAndVerifySPImessage(TEST_CONFIG_INSTANCE.getURN(), INPUT_SPI_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS, EXTERNAL_MESSAGE_INDIVIDUAL_DEFENDANT_ALL_FIELDS_JSON, TEST_CONFIG_INSTANCE.getRequestId(), TEST_CONFIG_INSTANCE.getCorrelationId());

        final String policeResultGenerated = readFileToString(new File(getClass().getClassLoader().getResource("external/message/public.results.police-result-generated.json").getFile()))
                .replace("URN_DEFAULT_VALUE", TEST_CONFIG_INSTANCE.getURN());

        // first CJSE submission gets an invalid response code, every later one succeeds
        stubCJSESoapService();
        postMessageToTopicAndVerify(policeResultGenerated, "public.results.police-result-generated", "stagingprosecutorsspi.event.cppmessage-retry-delay-required");
    }

    private int cjsePostsForThisUrn() {
        return findAll(postRequestedFor(urlMatching(CJSE_DELIVERY_URL)).withRequestBody(containing(TEST_CONFIG_INSTANCE.getURN()))).size();
    }

    // event names of the CPPMessage stream for this URN, in stream order; only cppmessage-prepared-for-sending carries the URN
    private static List<String> cppMessageStream() throws SQLException {
        try (Connection connection = connect(EVENTSTORE_JDBC_URL);
             PreparedStatement statement = connection.prepareStatement("select replace(name, 'stagingprosecutorsspi.event.', '') from event_log " +
                     "where stream_id = (select stream_id from event_log where name = 'stagingprosecutorsspi.event.cppmessage-prepared-for-sending' and payload like ? limit 1) " +
                     "order by position_in_stream")) {
            statement.setString(1, "%" + TEST_CONFIG_INSTANCE.getURN() + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                final List<String> events = new ArrayList<>();
                while (resultSet.next()) {
                    events.add(resultSet.getString(1));
                }
                return events;
            }
        }
    }

    private static void installFaultInjection(final String operation, final String table, final String timerFlowRow) throws SQLException {
        execute("create sequence " + FAULT_SEQUENCE,
                "create function " + FAULT_FUNCTION + "() returns trigger language plpgsql as $$\n" +
                        "begin\n" +
                        "  if " + timerFlowRow + " then\n" +
                        "    if nextval('" + FAULT_SEQUENCE + "') = 1 then\n" +
                        "      raise exception 'SPI-M01 fault injection: first timer_flow % on % fails', lower(TG_OP), TG_TABLE_NAME;\n" +
                        "    end if;\n" +
                        "  end if;\n" +
                        "  if TG_OP = 'DELETE' then\n" +
                        "    return old;\n" +
                        "  end if;\n" +
                        "  return new;\n" +
                        "end $$",
                "create trigger " + FAULT_TRIGGER + " before " + operation + " on " + table + " for each row execute function " + FAULT_FUNCTION + "()");
    }

    private static void removeFaultInjection() throws SQLException {
        execute("drop trigger if exists " + FAULT_TRIGGER + " on act_ru_job",
                "drop trigger if exists " + FAULT_TRIGGER + " on act_ru_variable",
                "drop function if exists " + FAULT_FUNCTION + "()",
                "drop sequence if exists " + FAULT_SEQUENCE);
    }

    private long faultedWrites() throws SQLException {
        return count("select case when is_called then last_value else 0 end from " + FAULT_SEQUENCE);
    }

    private static List<String> timerFlowJobs() throws SQLException {
        try (Connection connection = connect(VIEWSTORE_JDBC_URL);
             PreparedStatement statement = connection.prepareStatement("select id_, retries_, exception_msg_ " + TIMER_FLOW_JOBS);
             ResultSet resultSet = statement.executeQuery()) {
            final List<String> jobs = new ArrayList<>();
            while (resultSet.next()) {
                jobs.add(format("job %s retries=%d exception=%s", resultSet.getString(1), resultSet.getInt(2), resultSet.getString(3)));
            }
            return jobs;
        }
    }

    private static long count(final String sql) throws SQLException {
        try (Connection connection = connect(VIEWSTORE_JDBC_URL);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static void execute(final String... sqls) throws SQLException {
        try (Connection connection = connect(VIEWSTORE_JDBC_URL);
             Statement statement = connection.createStatement()) {
            for (final String sql : sqls) {
                statement.execute(sql);
            }
        }
    }

    private static Connection connect(final String url) throws SQLException {
        return DriverManager.getConnection(url, DB_USER_AND_PASSWORD, DB_USER_AND_PASSWORD);
    }
}
