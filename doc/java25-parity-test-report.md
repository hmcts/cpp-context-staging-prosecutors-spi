# Java 25 parity test report — staging-prosecutors-spi

**Branch:** `dev/java25-upgrade` (`d573c9a`), tests on `dev/java25-upgrade-java17-parity`.
**Baseline:** Java 17 `main` (`c02d92c`), tests on `dev/CIMD-4111-parity-with-j25`.
**Jira:** CIMD-4111. **Date:** 2026-10-15. **Updated:** 2026-10-27 — dispositions applied; every finding is fixed or accepted (see *Resolution*). **Updated:** 2026-10-29 — the team confirmed the Java 25 Activiti behaviour (SPI-M02, SPI-M04); the SPI-M03 test was removed, so SPI-M03 is accepted but no longer pinned by a test. **Updated:** 2026-11-03 — the SPI-M01 failure path is reproduced on Java 25 with fault injection (`ActivitiTimerJobRollbackParityIT`, 3 of 4 tests fail); see *Handover: SPI-M01 failure path*.

## What this is

These are characterisation ("golden master") tests, following the team guide *Parity Testing Java17 → Java25*. The same scenarios and assertions run against the Java 17 `main` branch and against this Java 25 branch:
- On **Java 17 every test passes**. That records how the service behaved before the upgrade.
- On **Java 25 a failing parity test is a behaviour change introduced by the upgrade.** Each one is either fixed in production code or explicitly accepted, in which case the Java 25 test is changed to pin the new behaviour.

Only the test harness and the `javax`/`jakarta` imports differ between the two copies. No production Java code was changed. The one build change is SPI-M05: the Function App's Java version in `stagingprosecutors-azure-functions/pom.xml`.

Every Java change in the upgrade itself is an import rename (`javax.*` → `jakarta.*`). The one exception is `Json.createArrayBuilder()` → `JsonObjects.createArrayBuilder()`. The behaviour changes below come from the persistence rewrite (DeltaSpike → JPA), the replaced Activiti engine, and dependency versions.

## Results

| Run | Tests | Failed | Notes |
|---|---|---|---|
| Java 17 `main` + parity tests, unit (JDK 17) | 354 | **0** | `DateTimeProviderTest` is flaky on `main`: it fails when run at exactly :00 seconds |
| Java 17 `main` + parity tests, `runIntegrationTests.sh` (WildFly 26) | 60 | **0** | 8 classes, 0 skipped |
| Java 25 this branch, unit (JDK 25), before resolution | 356 | **4** | all 4 are parity tests; every other test passes |
| Java 25 this branch, `runIntegrationTests.sh` (WildFly 40), before resolution | 60 | **3** | all 3 are `ActivitiParityIT`; every other IT passes |
| Java 25 this branch, unit (JDK 25), after resolution (2026-10-27) | 356 | **0** | |
| Java 25 this branch, `runIntegrationTests.sh` (WildFly 40), after resolution (2026-10-27) | 60 | **0** | full run 59/60; the `spiM03` URL then gained the `businessKey` parameter the library requires, and `ActivitiParityIT` was re-run alone: 3/3 |
| Java 25 this branch, `ActivitiParityIT` alone against the running WildFly 40 (2026-10-29) | 2 | **0** | after `spiM03` was removed; the full IT suite was not re-run |
| Java 25, `ActivitiTimerJobRollbackParityIT` alone against the running WildFly 40 (2026-10-29) | 4 | **3** | SPI-M01 failure path, see *Handover: SPI-M01 failure path*; not yet run on Java 17 |

## Behaviour changes found (tests failed on Java 25 before resolution)

The test names below are the original ones; see *Resolution* for the J25 names and what each now pins.

| ID | Severity | What changed | Test (failing on J25) | Recommendation |
|---|---|---|---|---|
| SPI-M05 | **Major** | `stagingprosecutors-azure-functions` is deployed as its own Function App on a **Java 17** runtime (`<java.version>17</java.version>`), but now bundles `utilities-core` 25.104.x, compiled for Java 25 (class-file version 69). `FileUtil`, `CpsPayloadTransformService`, `QueryMaterialAndSendNotificationService` and `RelayCaseToLibraFunction` would fail with `UnsupportedClassVersionError` on first use. CI does not catch it, because the unit tests run on JDK 25. | `azure-functions` `AzureFunctionsBytecodeLevelParityTest#spiM05_frameworkJsonObjectsShouldBeLoadableByTheJava17FunctionAppRuntime` (1) | **Fix.** Either keep a Java 17 build of `utilities-core` for this module, or move the Function App to a Java runtime that Azure supports and that can load the bytecode. |
| SPI-M02 | Medium | Activiti history level changed from `NONE` to `AUDIT`. The `activiti-embedded-rest` library hard-codes it. Every retry now writes `act_hi_procinst`, `act_hi_actinst` and `act_hi_varinst` rows, including `userId` and message ids, and nothing purges them. Seen live: 2 process instances and 6 variables after one IT run. | `integration-test` `ActivitiParityIT#spiM01_spiM02_…` (1) | **Accept or escalate** to the platform team (library setting). Agree a retention/purge approach if accepted. |
| SPI-M03 | Medium (security) | The library adds an Activiti REST API at `/stagingprosecutorsspi-service/internal/activiti/service/…` (process instances, jobs, history). On Java 17 it does not exist (404). On Java 25 it answers an **unauthenticated** request; it returns 500 here only because no process definition key was passed. | `integration-test` `ActivitiParityIT#spiM03_activitiRestApiShouldNotBeExposed` (1) | **Escalate** to the platform team. Confirm that `/internal/*` is blocked at the ingress in every environment, or disable the API. |
| SPI-M04 | Low | The `timer_flow` process definition is deployed by the library from `time_delay.bpmn20.xml` (content unchanged). Java 17 deployed it with activiti-cdi from `processes/time_delay.bpmn`. The first Java 25 start creates a new definition version; timers already in flight finish on the old one. | `integration-test` `ActivitiParityIT#spiM04_timerFlowShouldBeDeployedFromTimeDelayBpmn` (1) | **Accept.** Pin the new resource name in the J25 test. |
| SPI-P02 | Medium, environment-dependent | On Java 17 the framework `EntityManagerProducer`, which the DeltaSpike repositories used, set the JVM default time zone to UTC. Java 25 repositories use `@PersistenceContext` and never call it. If the JVM is not in UTC, `SpiOutMessage.timestamp` (a column without time zone) is stored as local time, and `findLatestSpiMessageForCaseUrnAndDefendantReference` can return the wrong "latest" message. The local WildFly 40 image runs with `-Duser.timezone=UTC`, so there is no impact there. | `viewstore-persistence` `ViewstoreRepositoryParityTest#spiP02_…` (2) | **Verify** that every environment's image sets UTC, then **Accept**, as casefile did for PCF-10. Otherwise fix by setting UTC explicitly. |
| SPI-P03 | Low | `save()` was DeltaSpike's "persist if new, else merge", which returns the same managed instance for a new entity. The Java 25 `save()` always merges and returns a managed copy, leaving the argument detached. No production caller uses the return value or changes the argument after `save`. | `viewstore-persistence` `ViewstoreRepositoryParityTest#spiP03_…` (1) | **Accept.** Pin the new contract in the J25 test. |

## Checked and unchanged (tests pass on both)

| ID | Area | Test |
|---|---|---|
| SPI-M01 (BC-09) | Activiti now uses a no-op transaction manager instead of JTA. In the normal retry flow the CJSE retry is still sent exactly once and no jobs are left behind. The difference only shows if the timer job fails after its JMS send. On Java 25 the send cannot be rolled back, so a job retry could resend to CJSE. That needs fault injection to reproduce. It is the same platform gap as casefile PCF-04. **Update 2026-11-03:** now reproduced with fault injection, so this is a behaviour change on the failure path; see *Handover: SPI-M01 failure path*. | `integration-test` `ActivitiParityIT#spiM01_spiM02_…` (exactly-once and job cleanup) |
| SPI-M07 (BC-20) | Access-control kbases load the same rules: COMMAND_API 5, QUERY_API 2. | `command-api` `CommandApiRuleCountParityTest`, `query-api` `QueryApiRuleCountParityTest` |
| SPI-M08 | The `send-spi-result` XML from `ResultProcessor` is byte-identical: JAXB 4 namespaces, element order and date formats are unchanged. | `event-processor` `ResultProcessorXmlParityTest` |
| SPI-M05 (module) | Before resolution, the module's own classes were still Java 17 bytecode. Now pinned at Java 25 with the rest of SPI-M05. | `azure-functions` `AzureFunctionsBytecodeLevelParityTest#spiM05_moduleClassesShouldTargetTheJava25FunctionAppRuntime` |

Also checked by review, with no test needed:
- BC-01/02: no single-result finders.
- BC-04: no primitive entity fields.
- BC-05: no JPQL `!= null` guards.
- BC-06: no lazy associations.
- BC-07: `liquibase.hub.mode` was removed.
- The JAXB model is unchanged: all 163 generated classes are identical apart from imports.
- `JsonArrayConverter` is unused.
- No expected/golden test files were changed on the Java 25 branch.

## Resolution (2026-10-27)

Each finding was settled the same way as cpp-context-material's parity fix (CIMD-4115). Where the difference comes from the platform and no caller depends on the old behaviour, the J25 test now pins the J25 behaviour. Where a consumer would see the difference, production code is fixed.

| ID | Decision | Change | J25 test now |
|---|---|---|---|
| SPI-M05 | **Fixed:** the Function App moves to Java 25 in the same release | `stagingprosecutors-azure-functions/pom.xml`: `java.version` is `${java.se.version}` again (as on `main` and in material), instead of the hard-coded `17`. The compiler `release`, the plugin `javaVersion` and the `JAVA_VERSION` app setting all become 25. | `spiM05_frameworkJsonObjectsShouldTargetTheJava25FunctionAppRuntime` and `spiM05_moduleClassesShouldTargetTheJava25FunctionAppRuntime` pin class-file version 69 |
| SPI-M02 | **Accepted** (platform library, as in prosecution-casefile on J17) | none | `spiM01_spiM02_retryShouldBeSentExactlyOnceAndLeaveNoActivitiJobsButAuditHistory`: one retry adds 1 `act_hi_procinst` row, at least 1 `act_hi_actinst` row and 3 `act_hi_varinst` rows; the exactly-once and job-cleanup checks are unchanged |
| SPI-M03 | **Accepted** (platform library, as in prosecution-casefile on J17) | none | No test. `spiM03` was removed from `ActivitiParityIT` on 2026-10-29, so nothing pins this behaviour. When it was last run, `GET …/runtime/process-instances?processDefinitionKey=timer_flow&businessKey=<uuid>` returned 200 without authentication, and 500 "Business key is null" when `businessKey` was missing. |
| SPI-M04 | **Accepted** | none | `spiM04_timerFlowShouldBeDeployedFromTimeDelayBpmn`: `timer_flow` is deployed from `time_delay.bpmn20.xml` |
| SPI-P02 | **Accepted:** the server runs in UTC, as material's maP02 | none | `spiP02_repositoryUseShouldLeaveTheJvmDefaultTimeZoneUnchanged`; `spiP02_shouldStoreSpiOutMessageTimestampAsUtcWallClock` keeps the J17 value with the database session in UTC (H2 fixes its session zone at connect, so the test runs `set time zone 'UTC'`) |
| SPI-P03 | **Accepted**, as material's maP03 | none | `spiP03_saveOfNewEntityShouldReturnAManagedEntityWithTheSameFields` (`samePropertyValuesAs`, and the returned entity is managed) |

Follow-ups that come with these decisions:
- **SPI-M05:** the deployed `fa-ste-casefilter` Function App, and its `JAVA_VERSION` setting wherever infrastructure manages it, must move to Java 25 in the same release as this jar. Confirm that Azure Functions offers Java 25 on Windows for the plan.
- **SPI-M03:** confirm that `/internal/*` is blocked at the ingress in every environment. The endpoint answers without authentication.
- **SPI-M02:** agree a retention/purge approach for the Activiti history tables. Each retry adds rows that include `userId` and message ids.
- **SPI-P02:** every environment's WildFly must run in UTC (`-Duser.timezone=UTC`, as the local WildFly 40 image does).

## Review item without a test

**SPI-P01.** The repositories now get their `EntityManager` from the container (`@PersistenceContext`) instead of the framework's request-scoped producer. With event-stream self-healing enabled, the framework's `EntityManagerFlushInterceptor` flushes the producer's EntityManager, which the repositories no longer write to. Listener SQL errors therefore surface at JTA commit, outside the framework's event error handling. This cannot be reproduced deterministically in a test. To restore the Java 17 behaviour, inject the framework `EntityManager` (`@Inject EntityManager`) in the repositories; otherwise accept and document it.

## How to run

```bash
# Java 25 unit tests (this branch): all pass
JAVA_HOME=<jdk-25> mvn -o clean verify -Denforcer.skipMoJLatestInterfacesRule=true

# Java 25 ITs: cpp-developers-docker on branch java-25; all pass
JAVA_HOME=<jdk-25> CPP_DOCKER_DIR=<cpp-developers-docker> \
  MAVEN_ARGS="-o -Denforcer.skipMoJLatestInterfacesRule=true" ./runIntegrationTests.sh

# One IT class against the running environment (the profile is required, otherwise no ITs run)
JAVA_HOME=<jdk-25> mvn -o -Denforcer.skipMoJLatestInterfacesRule=true verify -pl stagingprosecutorsspi-integration-test \
  -Pstagingprosecutorsspi-integration-test -DINTEGRATION_HOST_KEY=localhost -Dit.test=ActivitiParityIT
```

Run the same tests on `dev/CIMD-4111-parity-with-j25` with JDK 17, and cpp-developers-docker on `java-17` for the ITs, to see the baseline pass.

## Notes for maintainers

- When a fix lands, the matching parity test should start passing. Keep the tests as regression guards. Where a change was accepted, the J25 test pins the new behaviour, so a later change to it shows up as a failure.
- The J25 viewstore harness creates the schema with `hbm2ddl`. On Hibernate 6+ that makes `spi_out_message.timestamp` a column *with* time zone, so `spiP02_shouldStoreSpiOutMessageTimestampAsUtcWallClock` first changes it back to `TIMESTAMP`, to match Liquibase (`DATETIME`) and the Java 17 harness.
- The J17 viewstore harness (HSQLDialect over H2) cannot look a UUID-keyed entity up after the persistence context is cleared, so the parity tests never do that.

## Handover: SPI-M01 failure path (`ActivitiTimerJobRollbackParityIT`)

Added 2026-11-03. The test is `stagingprosecutorsspi-integration-test/src/test/java/uk/gov/moj/cpp/staging/prosecutors/spi/it/ActivitiTimerJobRollbackParityIT.java`. The same file runs unchanged on Java 17 and Java 25: it has no `javax`/`jakarta` imports.

### Why it exists

`ActivitiParityIT` covers SPI-M01 only in the normal flow, where nothing fails, so it passes on both sides. The risk is a timer job that fails *after* its JMS send. This class injects exactly that failure.

How the CJSE retry uses Activiti (the code is the same on both branches):
1. CJSE answers a submission with a retry code (200, 201 or 202). `CPPMessage.receiveSyncResponse` records `unsuccessful-sync-response-recorded` and `cppmessage-retry-delay-required`.
2. `CPPMessagePreparedProcessor.processTimerDelayRequired` calls `ActivitiService.startTimerProcess`, which starts the Activiti process `timer_flow` (`time_delay.bpmn20.xml`). This runs on the event processor's JMS thread, inside its JTA transaction.
3. When the timer is due (`cjse.retry.duration`: `PT5M` by default, `PT1S` in the local Docker stack), Activiti's async executor runs `ProcessTimerFinished`. The delegate sends `stagingprosecutorsspi.command.handler.receive-retry-delay-timer-finished` over JMS. Activiti then deletes the timer job, the 3 process variables and the execution.
4. `StagingSyncResponseHandler.receiveDelayTimerFinished` calls `CPPMessage.sendMessageForRetry`. That records `cppmessage-prepared-for-sending`, and the processor submits to CJSE again.

What changed in step 3:
- **Java 17:** `ProgrammaticProcessEngineConfiguration` builds a `CdiJtaProcessEngineConfiguration` on WildFly's `java:/TransactionManager`. Each timer job runs in one JTA transaction. The JMS send is released only on commit, and a failure rolls back the send together with every Activiti row.
- **Java 25:** the engine comes from the platform library `activiti-embedded-rest` (`ActivitiEngineConfiguration`; no change in that module from 25.104.4 to 25.104.5). It uses `NoOpPlatformTransactionManager` with `transactionsExternallyManaged=true`.
  - The async executor thread has no JTA transaction, so the JMS send goes out at once and each SQL statement commits on its own.
  - When a later statement fails, the no-op manager reports a rollback although nothing was undone. Activiti then runs the job again.
  - The comment in `ActivitiEngineConfiguration` says the manager is "never called for commit/rollback". That is not accurate: Activiti's `SpringTransactionInterceptor` calls it for every command.
- `CPPMessage.sendMessageForRetry` checks only `submitRequest != null && sendTries < retryLimit`. It does not check whether a submission has already succeeded, so a second timer-finished command makes another CJSE submission.

### How the test works

- **Before each test,** it waits until no `timer_flow` job with retries left is still pending, because another timer could consume the fault. It then records baselines: the number of `timer_flow` executions and of unfinished `timer_flow` rows in `act_hi_procinst`. The assertions use deltas, so rows left by earlier runs don't count.
- **Fault injection:** it installs a plpgsql `BEFORE INSERT` or `BEFORE DELETE` trigger on `act_ru_job` or `act_ru_variable` in `stagingprosecutorsspiviewstore`. The Activiti tables live there (`java:/DS.stagingprosecutorsspi`).
  - The trigger raises an exception for the first matching `timer_flow` row only.
  - The one-shot flag is a sequence (`spi_m01_fault_once`), because `nextval()` survives a rollback. A flag kept in a table would roll back with the failed attempt.
- **One real retry:** an SPI message goes in, then `public.results.police-result-generated` is posted on the topic. The CJSE WireMock stub (`CJSESoapServiceStubForRetry`) answers the first submission with a retry code and every later one with success.
- **Waits:**
  - up to 60 s for the fault to fire;
  - up to 120 s for the second CJSE POST;
  - up to 120 s for the timer jobs to finish;
  - then 15 s more, so a late duplicate POST still gets counted.
- **`tearDown`** drops the trigger, the function and the sequence.

Activiti 5.22 writes inserts, then updates, then deletes, in `EntityDependencyOrder`:
- On delete, the timer job row goes first, then the variables, then the execution.
- On insert the order is reversed, so the timer job row is the last row a process start writes.

That gives the four fault points:

| Test | Fault | Asserts | Java 17 (expected, not yet run) | Java 25 (2026-10-29) |
|---|---|---|---|---|
| `spiM01_failedTimerJobDeleteShouldNotSendTheCjseRetryTwice` | first `timer_flow` delete on `act_ru_job` (timer thread) | 2 CJSE POSTs; no `timer_flow` job left; no execution or unfinished history added | pass | **fail:** `CJSE POSTs for this URN=3` |
| `spiM01_failedTimerJobDeleteShouldRecordTheCjseRetryOnceInTheCppMessageStream` | same | the CPPMessage stream in `event_log` is exactly the 5 Java 17 events | pass | **fail:** 6 events; an extra `cppmessage-prepared-for-sending` after `cpp-message-sent-succeessfully-recorded` |
| `spiM01_failedVariableDeleteShouldLeaveNoOrphanTimerFlowExecution` | first `timer_flow` delete on `act_ru_variable` (timer thread) | as the first test | pass | **fail:** `timer_flow executions added=1` (POSTs 2) |
| `spiM01_failedTimerStartShouldLeaveNoPartialTimerFlowProcess` | first `timer_flow` insert on `act_ru_job` (processor thread, timer start) | as the first test | pass | pass |

What the Java 25 results mean:
- **Job delete fails:** the job row stays and Activiti runs the job again, but the first JMS send is already out. The command arrives twice, the aggregate has no guard, and CJSE gets a third submission: the same message, about 10 s after the second. The Activiti history shows 2 `processTimerFinished` runs for 1 timer.
- **Variable delete fails:** the job delete before it has already committed, so nothing is left to retry. CJSE gets the normal 2 POSTs, but the execution and its variables stay orphaned for good. This test does **not** show that the double send is absent; the job-delete tests show the double send.
- **Timer start fails:** passes on Java 25 too. The processor's JTA transaction includes the Activiti datasource, so the start rolls back as a unit, the event is redelivered and the retry completes. The problem is therefore limited to the async executor thread.
- **Viewstore:** not affected. The listener subscribes to no CPPMessage event, so the event store is the only store where the duplicate lands.

**Run details:**
- **Java 25:** run on 2026-10-29 on `dev/java25-upgrade-java17-parity` (`5597f3f`), with WildFly 40.0.1 and JDK 25. The 4 tests take about 3 minutes.
  - Between `5597f3f` and `team/25.104.x` (`7f19afb`), the only changes are version numbers (service parent and coredomain 25.104.4 → 25.104.5, the project version, the Azure Functions Java version), `ActivitiParityIT` and this report.
  - The SPI production code is identical, and so is `activiti-embedded-rest` across 25.104.4 → 25.104.5, so the result still applies.
- **Java 17:** not run yet, because the Java 17 stack was gone by the time the class was written. The class compiles on JDK 17 on `dev/CIMD-4111-parity-with-j25`.

### How to run

Prerequisites:
- SPI is deployed and healthy (`./runIntegrationTests.sh`), with cpp-developers-docker on branch `java-25`, or `java-17` for the Java 17 baseline.
- Postgres is reachable on `<INTEGRATION_HOST_KEY>:5432` with user and password `stagingprosecutorsspi`. The test creates a sequence, a function and a trigger in `stagingprosecutorsspiviewstore`; the context user owns the Activiti tables, so it has the rights.
- No other test runs against the same stack at the same time (for example `SPIResultRetryIT`). Another retry timer could take the one-shot fault.

```bash
# Java 25, against the running WildFly 40 (whole class, about 3 minutes)
JAVA_HOME=<jdk-25> mvn -B -o verify -pl stagingprosecutorsspi-integration-test -Pstagingprosecutorsspi-integration-test \
  -DINTEGRATION_HOST_KEY=localhost -DARTEMIS_HOST_KEY=localhost -DINTEGRATION_HOST_URI=http://localhost:8080 \
  -Denforcer.skip=true -Djacoco.skip=true -Dit.test=ActivitiTimerJobRollbackParityIT

# a single test
  ... -Dit.test='ActivitiTimerJobRollbackParityIT#spiM01_failedTimerJobDeleteShouldNotSendTheCjseRetryTwice'
```

Java 17 baseline: copy the class unchanged onto `dev/CIMD-4111-parity-with-j25`, deploy the Java 17 stack, and run the same command with JDK 17. All 4 tests are expected to pass.

Every failure message lists everything the test observed, for example `observed: CJSE POSTs for this URN=3, timer_flow jobs left=[], timer_flow executions added=0, unfinished timer_flow history added=0, writes seen by the fault trigger=2`. "Writes seen by the fault trigger" counts the matching writes: the first one failed and the rest passed.

### Checking by hand

```sql
-- stagingprosecutorsspiviewstore: timer jobs still pending or retrying
select id_, retries_, duedate_, exception_msg_ from act_ru_job where proc_def_id_ like 'timer_flow:%';

-- stagingprosecutorsspiviewstore: timer_flow executions (none expected once a retry has completed)
select id_, proc_inst_id_ from act_ru_execution where proc_def_id_ like 'timer_flow:%';

-- stagingprosecutorsspiviewstore, Java 25 only (Java 17 keeps no history): 2 rows for one process = the delegate ran twice
select proc_inst_id_, count(*) from act_hi_actinst
 where proc_def_id_ like 'timer_flow:%' and act_id_ = '_5' group by proc_inst_id_;

-- stagingprosecutorsspieventstore: the CPPMessage stream for a URN
select position_in_stream, name from event_log
 where stream_id = (select stream_id from event_log
                     where name = 'stagingprosecutorsspi.event.cppmessage-prepared-for-sending'
                       and payload like '%<URN>%' limit 1)
 order by position_in_stream;
```

If a run is killed before `tearDown`, the trigger stays and fails the next `timer_flow` write. To remove it:

```sql
drop trigger if exists spi_m01_fail_first_timer_flow_write on act_ru_job;
drop trigger if exists spi_m01_fail_first_timer_flow_write on act_ru_variable;
drop function if exists spi_m01_fail_first_timer_flow_write();
drop sequence if exists spi_m01_fault_once;
```

### Pitfalls

- **Column names differ:** the process instance column is `act_ru_job.process_instance_id_` but `act_ru_variable.proc_inst_id_`.
  - A trigger that references a missing column errors on every write, so the sequence never advances.
  - The test then times out waiting for the fault (`ConditionTimeoutException … expected a value equal to or greater than <1L> but <0L>`). This happened once while the class was written.
- **That broken run also leaves a DLQ message:** its timer start failed on every redelivery, so its `cppmessage-retry-delay-required` message ended in the local DLQ. This only affects the local stack.
- **The variable-delete test leaves one orphan `timer_flow` execution per Java 25 run.** That is the behaviour under test. The baselines keep later runs correct, and a fresh deploy drops the database.

### What makes it pass on Java 25

| Change | Expected effect on the 4 tests |
|---|---|
| Platform (`activiti-embedded-rest`): run each Activiti command in a real JTA transaction, using a `PlatformTransactionManager` over `jakarta.transaction.TransactionManager` instead of the no-op | all 4 pass: a failed attempt rolls back together with its JMS send |
| SPI only: `CPPMessage.sendMessageForRetry` emits nothing once a success is recorded | 3 of 4 pass. Both job-delete tests pass; the variable-delete test still leaves the orphan. The duplicate JMS command is still sent but does nothing. This works only because the success is recorded before the duplicate arrives, about 10 s later. |

Neither change has been tried yet. **Status:** on `team/25.104.x` this class fails 3 tests until a fix lands, so a full `runIntegrationTests.sh` run reports those failures. The team needs to decide whether to fix first or merge the test as a known failure. Pinning the Java 25 behaviour, as was done for SPI-M02/M04, would pin the duplicate CJSE submission.
