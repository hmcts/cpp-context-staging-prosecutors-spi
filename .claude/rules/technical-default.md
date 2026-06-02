# Service Identity

- **Service:** cpp-context-staging-prosecutors-spi
- **Description:** Staging/ingestion context. Receives prosecution cases from CJSE (Criminal Justice Services Exchange) over SOAP/REST, validates them against CJSE XSD, filters/transforms them via an Activiti-orchestrated workflow, and publishes filtered cases (as public events + commands) to the Prosecution Case File (PCF) context. A separate Azure Functions module performs serverless case filtering.
- **Bounded context:** `stagingprosecutorsspi` (one of many CPP contexts).
- **Programme:** Crime Common Platform (CPP).
- **Organisation:** HMCTS / Ministry of Justice.

## Technology Stack

| Component         | Value                                                                |
|-------------------|----------------------------------------------------------------------|
| Build tool        | Maven (multi-module reactor, 12 modules; root `pom.xml`)             |
| Language          | Java 17 (CI demand `centos8-j17`); Azure Functions on Java 11+ runtime |
| Framework         | Justice Services Framework / CPP `service-parent-pom:17.104.x` (CDI/Deltaspike) |
| Packaging         | WAR → WildFly via Docker (composite `stagingprosecutorsspi-service`); `stagingprosecutors-azure-functions` as a separate Azure Functions ZIP |
| Annotations       | `@ServiceComponent`, `@Handles`, `@ApplicationScoped`                 |
| Intake            | CJSE over SOAP (JAX-WS) + REST (JAX-RS)                               |
| Workflow          | Activiti BPMN engine (in the event processor)                        |
| Persistence       | Liquibase changelogs + Deltaspike repositories (event-store, aggregate-snapshot, viewstore, event-buffer) |
| Messaging         | JMS topics `stagingprosecutorsspi.event`, `public.event` (async publish pipeline) |
| Tests             | JUnit 5 + Mockito (unit); framework's IT harness (`runIntegrationTests.sh`); Awaitility, XMLUnit, JSONAssert |
| CI                | Azure DevOps Pipelines (`azure-pipelines.yaml` + `hmcts/cpp-azure-devops-templates`) |
| Quality gate      | SonarQube in CI (project `uk.gov.moj.cpp.staging.prosecutors.spi:stagingprosecutorsspi`) |
| Java packaging    | Root namespace `uk.gov.moj.cpp.staging.*`; Azure functions `uk.gov.moj.cpp.casefilter.*` |

## Constraints

- Maven is the current build tool. Future migration to Gradle is allowed but requires coordinating constitution + rule files + CI pipeline together (see Constitution Principle V).
- Java 17 (WARs); Azure Functions target Java 11+.
- Use `@ServiceComponent` + `@Handles` for command/event handling — NOT hand-rolled JMS listeners
- CDI (`@ApplicationScoped` / `@Inject`), never Spring DI (`@Autowired` / `@Component` / `@Service`)
- Aggregate state mutation must go through the aggregate's `apply(event)` replay (`SPIPoliceCase` / `CPPMessage` / `OIMessage` / `CJSEMessage`)
- Workflow orchestration via the Activiti BPMN engine inside the processor — not hand-rolled elsewhere
- Event listeners and processors must use converter classes in `converter/` packages — NOT inline mapping
- Contracts (RAML, JSON schemas, CJSE XSD, `subscriptions-descriptor.yaml`, `public-publications-descriptor.yaml`, `event-sources.yaml`) update FIRST, then regenerate codegen, then Java (Constitution Principle I)
- Schema additions / removals / renames update both the subscription/publication descriptor AND the JSON schema in lockstep (Constitution Principle VI)
- Never hand-edit generated POJO/JAXB sources — change the schema/XSD and regenerate
- Logging via SLF4J only — no `System.out` / `System.err` (Constitution Principle VII)
- Test-Driven Development is mandatory (Constitution Principle VIII)

## Build & Test Commands

```bash
# Full build + codegen + unit tests
mvn clean install

# Build, no tests
mvn clean install -DskipTests

# Unit tests only
mvn test

# Single module
mvn clean install -pl stagingprosecutorsspi-command-api

# Single unit test
mvn -pl <module> test -Dtest=ClassName#methodName

# Package Azure Functions
cd stagingprosecutors-azure-functions && mvn clean package

# Integration tests (requires Dockerised env up; CPP_DOCKER_DIR must be set)
./runIntegrationTests.sh

# Single IT against running env
mvn -pl stagingprosecutorsspi-integration-test test -Dit.test=ClassNameIT

# Framework JMX commands
./runSystemCommand.sh           # help / list
./runSystemCommand.sh CATCHUP   # run one
```

## Key version pins (`pom.xml`)

- Parent: `uk.gov.moj.cpp.common:service-parent-pom:17.104.x` (currently 17.104.0); artifact `stagingprosecutorsspi` (currently `17.104.49-SNAPSHOT`), groupId `uk.gov.moj.cpp.staging.prosecutors.spi`
- Cross-context / notable pins to keep aligned: `prosecutioncasefile` (downstream — the SPI→PCF contract), `coredomain`, `referencedata`, `cpp.system-users` / `system.users.library`, `system.id-mapper`, `stream.transformation.tool.anonymise`, `streambuffer`
- When bumping any of these, also check the matching schema/RAML classifier dep is on the same version
