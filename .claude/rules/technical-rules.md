# Coding Conventions — MOJ / CPP Standard (this service)

## Dependency Injection / Component Wiring

- Use the Justice Services Framework component model — `@ApplicationScoped` for framework-managed singletons, `@Inject` (CDI) for collaborator injection; Deltaspike for persistence
- For command handlers: `@ServiceComponent(COMMAND_HANDLER)` on the class + `@Handles("<command-name>")` on the method
- For event listeners: framework listener base + `@Handles("<event-name>")` on listener methods, `@ServiceComponent(EVENT_LISTENER)` on the class
- For event processors: framework processor base + `@ServiceComponent(EVENT_PROCESSOR)` on the class; multi-step orchestration via the Activiti BPMN engine wired into the processor module
- Do NOT use Spring annotations (`@Autowired`, `@Component`, `@Service`) — this service uses CDI, not Spring
- Do NOT roll your own JMS listener / JDBC connection / ObjectMapper / workflow state machine
- Do NOT hand-edit generated sources (POJOs from JSON schema, JAXB from CJSE XSD, adapters/clients from YAML) — change the contract and regenerate

## Envelope / Payload Handling

- Handler/listener method signatures take `Envelope<PayloadType>`, never the raw payload type
- Read the payload via `envelope.payload()`; metadata via `envelope.metadata()`
- Correlation context (correlation id, user id, etc.) lives in `envelope.metadata()` and should be propagated into MDC for SLF4J
- Treat the payload as immutable — do not mutate fields after reading

## Aggregate State Mutation

- All aggregate state mutation goes through the aggregate's `apply(event)` event-replay mechanism (`SPIPoliceCase`, `CPPMessage`, `OIMessage`, `CJSEMessage` under `stagingprosecutorsspi-domain`)
- The handler asks the aggregate to perform a command; the aggregate emits domain events; state is rebuilt by replaying those events via `apply(...)`
- Do NOT write events directly to the event store, and do NOT mutate read-model state from the command side

## CJSE XML / Intake

- Inbound CJSE messages arrive over SOAP (JAX-WS) and REST (JAX-RS); intake is via the generated command-api adapters (`command.api`, `command.api.soap`), not hand-written endpoints
- CJSE XML binding is generated from XSD in `stagingprosecutorsspi-cjse-schema` (JAXB) — change the XSD, not the generated types
- XSD validation failures must surface as the appropriate domain event/response (e.g. OI XSD-failure handling), never be swallowed

## Converters (Listener and Processor)

- Listener converters map domain events → viewstore entities (`viewstore.cpp_message`)
- Processor converters map SPI domain events → PCF (Prosecution Case File) payloads / public-event payloads
- Each converter is a single-purpose class (one event → one target shape); composition happens at the listener/processor level

## Error Handling

- Custom exceptions extend `RuntimeException` (or framework-specific bases like `EventStreamException`)
- NEVER swallow exceptions silently — always log or rethrow
- Listener / processor methods can let framework exceptions propagate; the framework handles redelivery and dead-letter routing; the event buffer reorders out-of-order events
- Invalid envelope payloads should fail loudly with a meaningful message — the framework re-delivers, so a silent skip leaks broken state

## Logging

- SLF4J with the framework's logger configuration
- Use `private static final Logger LOGGER = LoggerFactory.getLogger(...)`
- MDC keys: include correlation id and other relevant fields from `envelope.metadata()`
- NEVER use `System.out.println`, `System.err.println`, or `Throwable#printStackTrace()` (Constitution Principle VII)
- NEVER log sensitive data (case/defendant identifiers in plain text without masking, CJSE message content, tokens, passwords, PII)

## Imports

- NEVER use wildcard imports (`import java.util.*`) — always use explicit imports for each class

## Naming Conventions

| Component        | Pattern                  | Example                                  |
|------------------|--------------------------|------------------------------------------|
| Command handler  | `*Handler`               | `ReceiveProsecutionCaseHandler`          |
| Event listener   | `*Listener`              | `SpiEventListener`                       |
| Event processor  | `*Processor`             | `SpiProsecutionCaseProcessor`            |
| Converter        | `*Converter` or `*To*Converter` | (under `converter/`)              |
| Aggregate        | (singular noun)          | `SPIPoliceCase`, `CPPMessage`, `OIMessage`, `CJSEMessage` |
| Domain event     | (past tense)             | `SpiProsecutionCaseReceived`, `SpiResultPreparedForSending` |
| Azure function   | `*Function` / descriptive | `ApplyFilterRules`, `RelayCaseOnCPP`    |
| Test             | `*Test` / `*IT`          | `ReceiveProsecutionCaseHandlerTest`, `*IT` |

## Testing Conventions

- JUnit 5 + Mockito for unit tests
- `@Nested` classes with `@DisplayName` for grouped scenarios
- Method naming: `{action}_{scenario}_should_{expectation}`
- XMLUnit / JSONAssert for CJSE-XML and PCF-payload structural assertions; Awaitility for async event-flow assertions
- Integration tests live in `stagingprosecutorsspi-integration-test` and run via `./runIntegrationTests.sh`
- Test commands:
  - `mvn test` — unit tests only
  - `./runIntegrationTests.sh` — full Dockerised IT run
  - `mvn -pl stagingprosecutorsspi-integration-test test -Dit.test=ClassNameIT` — single IT against running env
- TDD: write the failing test first, see it fail for the right reason, then implement (Constitution Principle VIII)
- Logging in tests: SLF4J only (Constitution Principle VII)

## RAML / JSON Schema / XSD

- RAML files: `src/raml/...` per command/query module
- JSON schemas: `src/main/resources/json/` and `.../json/schema/`, single namespace `http://cpp.moj.gov.uk/staging/prosecutors/spi/json/schemas/...`; events carry the `x-event-name` extension
- CJSE XSDs: `stagingprosecutorsspi-cjse-schema`
- Every command in RAML has a matching `@Handles` method; every event in a `subscriptions-descriptor.yaml` has a matching listener / processor method; every published event in `public-publications-descriptor.yaml` is actually emitted
- Every event has a JSON schema; every JSON schema is referenced from at least one contract artefact
- When adding a new event:
  1. JSON schema first (with `x-event-name`)
  2. `subscriptions-descriptor.yaml` and/or `public-publications-descriptor.yaml` entry
  3. `event-sources.yaml` if a new topic is involved
  4. `mvn clean install` to regenerate the event POJO
  5. Java listener / processor method last
