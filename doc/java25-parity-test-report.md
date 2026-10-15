# Java 25 parity test report — staging-prosecutors-spi

**Branch:** `dev/java25-upgrade` (`d573c9a`), tests on `dev/java25-upgrade-java17-parity`.
**Baseline:** Java 17 `main` (`c02d92c`), tests on `dev/CIMD-4111-parity-with-j25`.
**Jira:** CIMD-4111. **Date:** 2026-10-15.

## What this is

These are characterisation ("golden master") tests, following the team guide *Parity Testing Java17 → Java25*. The same scenarios and assertions run against the Java 17 `main` branch and against this Java 25 branch:
- On **Java 17 every test passes**. That records how the service behaved before the upgrade.
- On **Java 25 a failing parity test is a behaviour change introduced by the upgrade.** Each one is either fixed in production code or explicitly accepted, in which case the Java 25 test is changed to pin the new behaviour.

Only the test harness and the `javax`/`jakarta` imports differ between the two copies. No production code was changed.

Every Java change in the upgrade itself is an import rename (`javax.*` → `jakarta.*`). The one exception is `Json.createArrayBuilder()` → `JsonObjects.createArrayBuilder()`. The behaviour changes below come from the persistence rewrite (DeltaSpike → JPA), the replaced Activiti engine, and dependency versions.

## Results

| Run | Tests | Failed | Notes |
|---|---|---|---|
| Java 17 `main` + parity tests, unit (JDK 17) | 354 | **0** | `DateTimeProviderTest` is flaky on `main`: it fails when run at exactly :00 seconds |
| Java 17 `main` + parity tests, `runIntegrationTests.sh` (WildFly 26) | 60 | **0** | 8 classes, 0 skipped |
| Java 25 this branch, unit (JDK 25) | 356 | **4** | all 4 are parity tests; every other test passes |
| Java 25 this branch, `runIntegrationTests.sh` (WildFly 40) | 60 | **3** | all 3 are `ActivitiParityIT`; every other IT passes |

## Behaviour changes found (tests fail on Java 25)

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
| SPI-M01 (BC-09) | Activiti now uses a no-op transaction manager instead of JTA. In the normal retry flow the CJSE retry is still sent exactly once and no jobs are left behind. The difference only shows if the timer job fails after its JMS send. On Java 25 the send cannot be rolled back, so a job retry could resend to CJSE. That needs fault injection to reproduce. It is the same platform gap as casefile PCF-04. | `integration-test` `ActivitiParityIT#spiM01_spiM02_…` (exactly-once and job cleanup) |
| SPI-M07 (BC-20) | Access-control kbases load the same rules: COMMAND_API 5, QUERY_API 2. | `command-api` `CommandApiRuleCountParityTest`, `query-api` `QueryApiRuleCountParityTest` |
| SPI-M08 | The `send-spi-result` XML from `ResultProcessor` is byte-identical: JAXB 4 namespaces, element order and date formats are unchanged. | `event-processor` `ResultProcessorXmlParityTest` |
| SPI-M05 (module) | The module's own classes are still Java 17 bytecode. | `azure-functions` `AzureFunctionsBytecodeLevelParityTest#spiM05_moduleClassesShould…` |

Also checked by review, with no test needed:
- BC-01/02: no single-result finders.
- BC-04: no primitive entity fields.
- BC-05: no JPQL `!= null` guards.
- BC-06: no lazy associations.
- BC-07: `liquibase.hub.mode` was removed.
- The JAXB model is unchanged: all 163 generated classes are identical apart from imports.
- `JsonArrayConverter` is unused.
- No expected/golden test files were changed on the Java 25 branch.

## Review item without a test

**SPI-P01.** The repositories now get their `EntityManager` from the container (`@PersistenceContext`) instead of the framework's request-scoped producer. With event-stream self-healing enabled, the framework's `EntityManagerFlushInterceptor` flushes the producer's EntityManager, which the repositories no longer write to. Listener SQL errors therefore surface at JTA commit, outside the framework's event error handling. This cannot be reproduced deterministically in a test. To restore the Java 17 behaviour, inject the framework `EntityManager` (`@Inject EntityManager`) in the repositories; otherwise accept and document it.

## How to run

```bash
# Java 25 unit tests (this branch): expect exactly the 4 parity failures above
JAVA_HOME=<jdk-25> mvn -o clean verify -Denforcer.skipMoJLatestInterfacesRule=true -Dmaven.test.failure.ignore=true

# Java 25 ITs: cpp-developers-docker on branch java-25. The failure-ignore flag lets the
# build reach the ITs despite the red parity unit tests; expect the 3 ActivitiParityIT failures
JAVA_HOME=<jdk-25> CPP_DOCKER_DIR=<cpp-developers-docker> \
  MAVEN_ARGS="-o -Denforcer.skipMoJLatestInterfacesRule=true -Dmaven.test.failure.ignore=true" ./runIntegrationTests.sh
```

Run the same tests on `dev/CIMD-4111-parity-with-j25` with JDK 17, and cpp-developers-docker on `java-17` for the ITs, to see the baseline pass.

## Notes for maintainers

- When a fix lands, the matching parity test should start passing. Keep the tests as regression guards.
- The J25 viewstore harness creates the schema with `hbm2ddl`. On Hibernate 6+ that makes `spi_out_message.timestamp` a column *with* time zone, so `spiP02_shouldStoreSpiOutMessageTimestampAsUtcWallClock` first changes it back to `TIMESTAMP`, to match Liquibase (`DATETIME`) and the Java 17 harness.
- The J17 viewstore harness (HSQLDialect over H2) cannot look a UUID-keyed entity up after the persistence context is cleared, so the parity tests never do that.
