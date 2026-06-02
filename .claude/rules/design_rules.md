# Architecture & Domain Rules

## Three Layers (CQRS / Event-Sourced)

```
1. Command side (SOAP/REST intake → handler → aggregate → domain event)
       ↓ writes to event store (java:/app/stagingprosecutorsspi-service/DS.eventstore)
       ↓ async publish pipeline → JMS topic stagingprosecutorsspi.event

2. Event listener (projects events → viewstore tables)
       ↓ projects to the viewstore (viewstore.cpp_message)

3. Event processor (Activiti workflow → SPI→PCF transform)
       ↓ sends commands to Prosecution Case File + publishes public.event
```

Every change touching events MUST be reasoned about across **all three layers**. Breaking one without the others produces silent data drift — and here the drift escapes the context boundary into Prosecution Case File.

- **Command side** — CJSE messages arrive over SOAP (JAX-WS) / REST, routed by generated command-api adapters to `@Handles` handlers (`hmcts.cjs.receive-spi-message`, `hmcts.cjs.resend-message`). Handlers ask the aggregate to perform a command; the aggregate emits domain events. State is rebuilt by replaying events via `apply(...)`.
- **Event listener** — projects domain events into the viewstore (`viewstore.cpp_message`). Lives under `stagingprosecutorsspi-event/stagingprosecutorsspi-event-listener`. Converters map events → viewstore entities.
- **Event processor** — consumes domain events, orchestrates multi-step case processing via the **Activiti BPMN** engine, transforms SPI → PCF (Prosecution Case File) format, sends commands to PCF, and publishes public events. Lives under `stagingprosecutorsspi-event/stagingprosecutorsspi-event-processor`. Heavy use of converters (20+ classes).

## Domain Concepts

| Concept                 | Description                                                                                                                              |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| SPIPoliceCase           | Core aggregate for an incoming police/prosecution case. States: LIVE, EJECTED. Commands: `receivePoliceCase`, `filterProsecutionCase`, `handleEjectCase`. |
| CPPMessage              | Aggregate for the message prepared for CJSE/CPP transmission. Commands: `prepareCPPMessageForSending`, `resendMessage`.                  |
| OIMessage               | Operational Interface aggregate. Commands: `oiRequestReceived`, `updatePoliceSystemId`, `prepareOIResponseForXSDFailures`.               |
| CJSEMessage             | Aggregate representing the raw CJSE message envelope.                                                                                     |
| Domain event            | Internal event written to the event store. Examples: `prosecution-case-received`, `spi-result-prepared-for-sending`, `spi-oi-police-system-updated`, `spi-oi-request-received`. |
| Public event            | Cross-context event published on `public.event` via `public-publications-descriptor.yaml` (e.g. `public.stagingprosecutorsspi.event.prosecution-case-filtered`). |
| Command                 | Inbound request via SOAP/REST → `staging-prosecutors-command-handler.messaging.raml` → `@Handles`.                                       |
| Listener                | Read-side projection — `*Listener` extending the framework listener base; projects events → viewstore via converters.                    |
| Processor               | Activiti-orchestrated side-effect + public-event emitter — `*Processor` extending the framework processor base; SPI→PCF transform.        |
| Azure filter function   | HTTP-triggered Azure Function (`uk.gov.moj.cpp.casefilter.*`) applying filter rules (court centre, prosecutor code, case initiation type) → eject/filter/relay. |
| Viewstore               | Read model (`viewstore.cpp_message`), populated by listeners. Schema managed by `stagingprosecutorsspi-viewstore-liquibase`.             |
| Event store             | Append-only log `DS.eventstore`. Source of truth for aggregate state. Async publish pipeline (`pre_publish_queue` → `published_event` → `publish_queue`). |
| Event buffer            | `event_buffer` schema — buffers out-of-order JMS events (`stream_buffer`) and tracks processed events (`processed_event`).                |

## Authoritative Routing Files (always re-read before reasoning about a flow)

- `stagingprosecutorsspi-event-sources/src/yaml/event-sources.yaml` — event-source streams (`stagingprosecutorsspi.event.source`, `public.event.source`) + JMS/REST URIs and datasource.
- `stagingprosecutorsspi-event/stagingprosecutorsspi-event-listener/src/yaml/subscriptions-descriptor.yaml` — listener subscriptions.
- `stagingprosecutorsspi-event/stagingprosecutorsspi-event-processor/src/yaml/subscriptions-descriptor.yaml` — processor subscriptions.
- `stagingprosecutorsspi-event/stagingprosecutorsspi-event-processor/src/yaml/public-publications-descriptor.yaml` — published public events.
- `stagingprosecutorsspi-command/stagingprosecutorsspi-command-handler/src/raml/staging-prosecutors-command-handler.messaging.raml` — command → handler mapping.
- `stagingprosecutorsspi-command/stagingprosecutorsspi-command-api/src/raml/staging-prosecutors-command-api.raml` and the two query RAML files.
- `stagingprosecutorsspi-cjse-schema` — CJSE XSDs (JAXB).
- `stagingprosecutorsspi-service/src/main/descriptors/resource-descriptor.yml` — datasources, JMS topics, service mapping.
- Per-command/per-event JSON schemas (single namespace `cpp.moj.gov.uk/staging/prosecutors/spi`, with `x-event-name`).

## Module Layout

- `stagingprosecutorsspi-command/-command-api` — RAML + JSON schemas; SOAP/REST intake adapters (`command.api`, `command.api.soap`)
- `stagingprosecutorsspi-command/-command-handler` — `@Handles` command handlers
- `stagingprosecutorsspi-domain` — `domain-aggregates` (`SPIPoliceCase`, `CPPMessage`, `OIMessage`, `CJSEMessage`), `domain-events`, `domain-value-schema`, `domain-transformation` (incl. anonymisation)
- `stagingprosecutorsspi-event/-event-listener` — listeners + converters → viewstore
- `stagingprosecutorsspi-event/-event-processor` — Activiti-orchestrated processors + converters → PCF + public events
- `stagingprosecutorsspi-event-sources` — `event-sources.yaml`
- `stagingprosecutorsspi-query/-query-api`, `-query-view` — query RAML + read views over the viewstore
- `stagingprosecutorsspi-cjse-schema` — JAXB/JAX-WS code generation from CJSE XSDs
- `stagingprosecutorsspi-validation-rules` — CJSE message validation logic
- `stagingprosecutorsspi-viewstore` — Liquibase migrations + Deltaspike repositories for the viewstore
- `stagingprosecutorsspi-service` — composite packaging WAR; `resource-descriptor.yml` wires datasources / topics
- `stagingprosecutorsspi-healthchecks` — health monitoring endpoints
- `stagingprosecutors-azure-functions` — 16+ HTTP-triggered case-filtering Azure Functions (`uk.gov.moj.cpp.casefilter.*`)
- `stagingprosecutorsspi-integration-test` — `*IT.java` orchestrated by `runIntegrationTests.sh`

## Adding a New Command

1. **RAML first.** Add the command to `staging-prosecutors-command-handler.messaging.raml` (and the intake API RAML) with the right media type.
2. **JSON schema.** Add the command payload schema (single namespace), and update the CJSE XSD if the inbound XML shape changes.
3. **Regenerate.** `mvn clean install` to generate the POJO / JAXB types.
4. **Handler.** Add `@Handles("<command-name>")` on a `@ServiceComponent(COMMAND_HANDLER)` class; method takes `Envelope<CommandPayload>`.
5. **Aggregate.** If the command mutates state, the handler asks the aggregate to perform it; the aggregate emits a domain event and rebuilds state via `apply(event)`.
6. **Listener.** If the new event updates the viewstore: subscription entry + JSON schema + listener method + converter.
7. **Processor.** If the new event triggers the Activiti workflow / SPI→PCF transform / a public event: subscription (or publication) entry + JSON schema + processor method + converter.
8. **Tests.** Failing unit tests for handler, aggregate, listener (if touched), processor (if touched), converters (if touched). Then production code. Then IT exercising the end-to-end flow.

## Adding a New Domain Event

- Add the event's JSON schema (single namespace, with `x-event-name`).
- Update the listener AND/OR processor `subscriptions-descriptor.yaml` (the two subscribe to overlapping but not identical sets — wire it to the component(s) that consume it; document any that are unaffected).
- For a published public event, add it to `public-publications-descriptor.yaml`.
- Update `event-sources.yaml` if a new topic is introduced.
- `mvn clean install` to regenerate the event POJO, then add the listener/processor method + converter and the failing-then-passing tests.

## Adding a Public-Event Subscription (incoming from another context)

1. **Subscription entry.** Add to listener and/or processor `subscriptions-descriptor.yaml` for the `public.event.source` (e.g. PCF's `public.prosecutioncasefile.*`).
2. **JSON schema.** Add the public-event schema (matches the upstream context's contract version).
3. **Listener / processor method.** With `@Handles("<public-event-name>")` and `Envelope<PayloadType>`.
4. **Converter.** Map the public-event payload → either a viewstore entity (listener) or a domain command (if it triggers a state change).
5. **Tests.** Unit tests for the listener/processor + converter. IT simulating the public-event arrival.

## Out-of-Scope (do not add)

- Hand-rolled JMS listeners — use the framework's `@Handles`
- Hand-rolled JDBC — use Liquibase changelogs and Deltaspike repositories
- Hand-rolled workflow state machines — use the Activiti BPMN engine in the processor
- Hand-edited generated sources (POJOs, JAXB) — change the schema/XSD and regenerate
- Ad-hoc `ObjectMapper` instances — use the framework's configured mapper
- Manual JSON schema validation — the framework validates incoming envelopes against subscription-declared schemas
- Spring annotations (`@Autowired`, `@Component`, `@Service`) — this service uses CDI
- Cross-context coupling beyond declared public events / the PCF command contract

## Common Gotchas

1. **Schema-subscription drift** — adding a `subscriptions-descriptor.yaml` / `public-publications-descriptor.yaml` entry without the matching JSON schema produces a runtime 500 on dispatch. Constitution Principle VI makes this a review-blocker.
2. **Three-layer drift** — modifying a domain event without updating the listener AND processor (where each consumes it) is the most common silent-data-drift bug, and here it leaks into Prosecution Case File. Constitution Principle II makes this a review-blocker.
3. **Hand-editing generated code** — POJOs (JSON schema) and JAXB types (CJSE XSD) are generated; edits are lost on the next `mvn clean install`. Change the contract instead.
4. **Liquibase registration** — adding a changelog file without registering it in the right registry (event-store / aggregate-snapshot / viewstore / event-buffer) means it never applies in CI's IT setup.
5. **Out-of-order events** — the `event_buffer` reorders events; listeners/processors must be idempotent and tolerate buffered redelivery.
6. **Wrong `@ServiceComponent` value** — `COMMAND_HANDLER` vs `EVENT_LISTENER` vs `EVENT_PROCESSOR` are NOT interchangeable; the framework dispatches based on the value.
7. **Cross-context pin drift** — bumping `prosecutioncasefile` / `coredomain` / `referencedata` / `system.*` versions in `pom.xml` requires bumping the matching schema/RAML classifier dep to the same version, or the SPI→PCF contract drifts.
