<!--
SYNC IMPACT REPORT
==================
Version change: (uninitialised template) → 1.0.0
Bump rationale: Initial ratification. All principles and sections are new; no
                prior principles to remove or redefine, so MAJOR is the correct
                starting point (1.0.0).

Modified principles: N/A (initial ratification).

Added sections:
  - Core Principles
      I.    RAML / JSON-Schema / CJSE-XSD Contract First
      II.   CQRS Three-Layer Discipline (Command / Listener / Processor)
      III.  CPP Framework Idioms — No Manual Rolling
      IV.   Spec-Driven Build Loop
      V.    HMCTS CPP Standards Compliance
      VI.   Schema-Subscription Symmetry
      VII.  No System.out / System.err — SLF4J Only
      VIII. Test-Driven Development
  - Technology Stack & Deployment
  - Development Workflow & Quality Gates
  - Governance

Removed sections: None.

Templates requiring updates:
  - .specify/templates/plan-template.md       ✅ compatible — the "Constitution
      Check" block is filled per-feature by `/speckit-plan`. Plan authors MUST
      gate on Principles I–VIII.
  - .specify/templates/spec-template.md       ✅ compatible.
  - .specify/templates/tasks-template.md      ✅ compatible — task ordering
      already encodes "tests before implementation", aligning with VIII.
  - .specify/templates/checklist-template.md  ✅ compatible.
  - README.md / CLAUDE.md / docs/*            ✅ aligned — `.claude/rules/*.md`
      encodes these principles informally; this constitution is now the
      authoritative source.

Follow-up TODOs: None. All placeholders resolved.
-->

# cpp-context-staging-prosecutors-spi Constitution

## Core Principles

### I. RAML / JSON-Schema / CJSE-XSD Contract First (NON-NEGOTIABLE)

The contracts of this service — commands it accepts (SOAP/REST from CJSE),
queries it answers, domain events it emits, public events it publishes, and the
CJSE XML messages it parses — are defined in **RAML files, JSON schemas, and
CJSE XSDs**. Those artefacts are the source of truth. Java handler signatures,
listener mappings, processor mappings, and generated POJOs/JAXB types MUST
follow the contracts; the contracts MUST NOT be inferred from the Java code.

For every command/event change you MUST update:

1. The RAML file — `staging-prosecutors-command-api.raml` (intake API),
   `staging-prosecutors-command-handler.messaging.raml` (command → media-type
   mapping), `stagingprosecutors-query-api.raml` / `stagingprosecutors-query-view.raml`
   (read side), and the relevant `subscriptions-descriptor.yaml` /
   `public-publications-descriptor.yaml` for event subscriptions and publications.
2. The matching JSON schema under the single namespace
   `http://cpp.moj.gov.uk/staging/prosecutors/spi/json/schemas/...`
   (referenced by `schema_uri`; events carry an `x-event-name` extension).
3. The CJSE XSD under `stagingprosecutorsspi-cjse-schema` when the change touches
   inbound/outbound CJSE XML (JAXB types are generated from it).
4. The `event-sources.yaml` if a new internal/public topic is involved.
5. Then — and only then — the Java handler / listener / processor (regenerate
   POJOs/JAXB with `mvn clean install` on the affected module first).

**Rationale**: the CPP framework dispatches commands and events by matching the
RAML/`schema_uri` contract against `@Handles` annotations, and code is generated
from these artefacts (`pojo-generation-plugin`, `maven-jaxb2-plugin`,
`messaging-adapter-generator-plugin`, `rest-client-generator-plugin`). A drift
between the contract and the Java code produces a runtime 500 (no matching
schema), a codegen failure, or silent message-loss with no logging. The
contracts are also consumed by CJSE and by the downstream Prosecution Case File
context; treating them as documentation rather than source-of-truth produces
cross-context incidents.

### II. CQRS Three-Layer Discipline (NON-NEGOTIABLE)

Every change touching events MUST be reasoned about across **all three
layers**:

```
Command side (SOAP/REST intake → handler → aggregate → domain event)
    ↓ writes events to DS.eventstore (async publish pipeline → stagingprosecutorsspi.event)
Event listener (projects events → viewstore tables)
    ↓ projects to the viewstore (viewstore.cpp_message)
Event processor (consumes domain events → Activiti workflow → transform SPI→PCF)
    ↓ sends commands to Prosecution Case File + publishes public.event
```

The aggregates are `SPIPoliceCase` (LIVE/EJECTED), `CPPMessage`, `OIMessage`,
and `CJSEMessage`; state is rebuilt by replaying events via `apply(...)`. Adding
or modifying a domain event WITHOUT updating both the listener and the processor
is a Principle II violation. Plan authors MUST list which of the three layers a
change touches and confirm the other two are either unaffected (with reasoning)
or carry a paired change in the same PR.

**Rationale**: the read-model (`viewstore.cpp_message`) and the downstream PCF
context depend on the listener and processor staying in lockstep with the
command side. The processor's Activiti-orchestrated SPI→PCF transformation is
where a dropped or mis-shaped field silently corrupts the case that reaches
Prosecution Case File — breaking one layer without the others produces silent
data drift.

### III. CPP Framework Idioms — No Manual Rolling (NON-NEGOTIABLE)

This service is built on `uk.gov.moj.cpp.common:service-parent-pom` (Justice
Services Framework). Use the framework's idioms rather than rolling your own:

- Command handlers: `@ServiceComponent(COMMAND_HANDLER)` + `@Handles(...)` on a
  method taking `Envelope<CommandPayload>`. Intake (SOAP/JAX-WS + REST) routes
  through the generated command-api adapters, not hand-written endpoints.
- Aggregate state: route mutations through the aggregate's `apply(event)` replay
  (`SPIPoliceCase` / `CPPMessage` / `OIMessage` / `CJSEMessage`); never mutate
  read-model state from the command side.
- Event listeners: extend the framework's listener bases; map events → JPA /
  Deltaspike viewstore entities via dedicated converter classes.
- Event processors: extend the framework's processor bases; orchestrate
  multi-step case processing via the **Activiti BPMN** engine wired into the
  processor module — do not hand-roll workflow state machines elsewhere.
- Persistence: Liquibase changelogs + Deltaspike repositories only — never
  manual DDL.
- Outbound: use the framework's REST client wiring / generated clients (PCF,
  reference-data, id-mapper, system-users); publish public events via the
  `public-publications-descriptor.yaml` contract.
- Codegen: POJOs from JSON schema, JAXB from CJSE XSD, adapters/clients from
  YAML descriptors — regenerate with `mvn clean install`, never hand-edit
  generated sources.

**Forbidden**: hand-rolled JMS listeners, hand-rolled JDBC, ad-hoc ObjectMapper
instances, manual schema validation, and Spring DI annotations (`@Autowired`,
`@Component`, `@Service`) — this service uses CDI (`@ApplicationScoped` /
`@Inject`), not Spring.

**Rationale**: every CPP service follows these idioms, so cross-service
maintenance and operability depend on consistency. A bespoke pattern in one
service makes the next maintainer reach for the wrong mental model.

### IV. Spec-Driven Build Loop (NON-NEGOTIABLE)

Every non-trivial change MUST flow through the cycle:

```
Spec → Write → Code Review → QA → Spec-Validate → Fix → Ship
```

The reviewer agents (`code-reviewer`, `qa`, `spec-validator`) report findings
only; they MUST NOT modify code. The primary agent or a human applies fixes,
then re-runs the loop until all three return PASS / COMPLIANT. The
`spec-validator` here checks that RAML, JSON-schema, and CJSE-XSD files are
consistent with both `subscriptions-descriptor.yaml` files, the
`public-publications-descriptor.yaml`, `event-sources.yaml`, and the Java
handler / listener / processor / converter mappings. Changes exempt from the
loop: markdown-only edits, whitespace or import-only edits, `.claude/rules/*`
and `CLAUDE.md` rule updates.

**Rationale**: keeps a human (or primary agent) as the decision point; prevents
conflicting auto-fixes; preserves auditable, reproducible review output.

### V. HMCTS CPP Standards Compliance (NON-NEGOTIABLE)

- **Build tool**: Maven (current). Module layout, version management, and CI all
  assume the Maven reactor; a future migration to Gradle is allowed but is
  itself a constitution-amendment-scale change and MUST update this section, the
  rule files, the agent docs, and the CI pipeline in lockstep.
- **Java**: 17 (CI demand `centos8-j17`). The Azure Functions module targets the
  Azure Java runtime (Java 11+) and is packaged separately.
- **Parent**: `uk.gov.moj.cpp.common:service-parent-pom:17.104.x` — pin updates
  require a coordinated cross-context check against the upstream pins in the root
  `pom.xml` (`prosecutioncasefile`, `coredomain`, `referencedata`,
  `cpp.system-users` / `system.users.library`, `system.id-mapper`,
  `stream.transformation.tool.anonymise`, `streambuffer`).
- **Packaging**: WAR modules deployed to WildFly via Docker; the
  `stagingprosecutorsspi-service` module is the composite WAR;
  `src/main/descriptors/resource-descriptor.yml` wires datasources / queues /
  topics / service mapping. `stagingprosecutors-azure-functions` is a separate
  Azure Functions artefact (16+ HTTP-triggered case-filtering functions,
  package `uk.gov.moj.cpp.casefilter.*`), not part of the WAR.
- **Tests**: JUnit 5 + Mockito for unit tests; integration tests in
  `stagingprosecutorsspi-integration-test` orchestrated by
  `runIntegrationTests.sh` (Docker-based WildFly + Postgres + ActiveMQ +
  WireMock; supporting libs: Awaitility, XMLUnit, JSONAssert). ITs require
  `CPP_DOCKER_DIR` pointing at a local checkout of `hmcts/cpp-developers-docker`.
- **CI/CD**: Azure DevOps (`azure-pipelines.yaml`) using shared
  `hmcts/cpp-azure-devops-templates`: PR builds run `context-verify`; CI builds
  run `context-validation` with `serviceName=stagingprosecutorsspi` and
  `itTestFolder=stagingprosecutorsspi-integration-test`. SonarQube project
  `uk.gov.moj.cpp.staging.prosecutors.spi:stagingprosecutorsspi`. `main` is the
  develop branch; `dev/release-*` branches are excluded (jgitflow).
- **Quality gate**: SonarQube — coverage, duplication, smells. No local
  Checkstyle / PMD enforcement at build time.

**Rationale**: aligns this service with the rest of the CPP estate (naming,
build, deploy, test, observability conventions) so cross-team maintenance,
on-call rotation, and platform upgrades work uniformly.

### VI. Schema-Subscription Symmetry (NON-NEGOTIABLE)

When you add, remove, or rename a domain or public event you MUST update **all**
of the relevant contracts in lockstep:

- The listener `subscriptions-descriptor.yaml`
  (`stagingprosecutorsspi-event/stagingprosecutorsspi-event-listener/src/yaml/...`)
  and/or the processor `subscriptions-descriptor.yaml`
  (`.../stagingprosecutorsspi-event-processor/src/yaml/...`) for consumed events.
- The processor `public-publications-descriptor.yaml` for **published** public
  events (e.g. `public.stagingprosecutorsspi.event.prosecution-case-filtered`).
- The matching JSON schema under
  `http://cpp.moj.gov.uk/staging/prosecutors/spi/json/schemas/...`, with the
  `x-event-name` extension set.

A subscription or publication without a matching schema produces a runtime 500
on dispatch. A schema without a subscription/publication is dead code that
drifts silently as the event evolves.

**Rationale**: this service both consumes and publishes events across the JMS
topics and the cross-context boundary to Prosecution Case File. A missing or
mismatched contract is the most common source of incidents on this service.
Encoding it as a NON-NEGOTIABLE principle (rather than a "common gotcha" in
CLAUDE.md) makes it a review-blocker.

### VII. No `System.out` / `System.err` — SLF4J Only (NON-NEGOTIABLE)

Code MUST NOT use `System.out.println`, `System.err.println`, or
`Throwable#printStackTrace()`. All diagnostic output goes through SLF4J
(`org.slf4j.Logger` via `LoggerFactory.getLogger(...)`). This applies to
production code AND tests.

**Rationale**: container logs are aggregated and structured; stdout prints
bypass the framework's MDC (correlation id propagation through the `Envelope`
metadata) and the platform log shipping. They vanish from operations and surface
as noise in CI.

### VIII. Test-Driven Development (NON-NEGOTIABLE)

Red → Green → Refactor for every behaviour change.

1. Write the failing test first. It MUST run and fail for the *correct* reason —
   the assertion, not a missing class or compilation error.
2. Write the minimum production code to make it pass.
3. Refactor with the test still green.

PRs MUST show that the test was authored at or before the production code
(commit history or paired-commit are both acceptable). The `qa` reviewer agent
gates on this — production code without an accompanying failing-then-passing
test is FAIL.

Exempt: pure mechanical refactors (rename, move, extract with no behaviour
change), formatting, comment-only edits, and regenerated codegen output.

**Rationale**: the regression surface of this service is wide — four aggregates,
SOAP/REST + CJSE-XML intake, an Activiti-orchestrated SPI→PCF transformation,
20+ processor classes, 16+ Azure filtering functions, and cross-context public
events. Only fail-first tests catch the class of bug where a converter silently
drops a field or a filter rule mis-routes a case.

## Technology Stack & Deployment

- **Java**: 17 (WildFly WARs); Azure Functions on the Azure Java runtime.
- **Build**: Maven. Multi-module reactor (12 modules); root `pom.xml`
  (`stagingprosecutorsspi`, groupId `uk.gov.moj.cpp.staging.prosecutors.spi`).
- **Framework**: Justice Services Framework / CPP `service-parent-pom:17.104.x`.
  CDI + Deltaspike; `@ServiceComponent` / `@Handles` annotations.
- **Intake**: CJSE over SOAP (JAX-WS) and REST (JAX-RS); handlers
  `hmcts.cjs.receive-spi-message`, `hmcts.cjs.resend-message`.
- **Workflow**: Activiti BPMN engine in the event processor.
- **Persistence**: Liquibase changelogs + Deltaspike repositories (event store,
  aggregate snapshot, event buffer, viewstore).
- **Messaging**: JMS topics `stagingprosecutorsspi.event` and `public.event`
  (async publish pipeline: `pre_publish_queue` → `published_event` →
  `publish_queue`).
- **Data stores**:
  - `java:/app/stagingprosecutorsspi-service/DS.eventstore` — event store.
  - viewstore — read model (`viewstore.cpp_message`); `event_buffer` schema for
    out-of-order buffering and processed-event tracking.
- **Cloud**: Azure Functions, Azure Storage (Tables/Blobs), Azure Event Grid.
- **Codegen**: `pojo-generation-plugin` (JSON schema), `maven-jaxb2-plugin`
  (CJSE XSD), `messaging-adapter-generator-plugin`, `rest-client-generator-plugin`.
- **Tests**: JUnit 5 + Mockito (unit); `runIntegrationTests.sh` Dockerised IT
  harness (WildFly + Postgres + ActiveMQ + WireMock; Awaitility, XMLUnit,
  JSONAssert).
- **Logging**: SLF4J + the framework's logger configuration; MDC keys carried
  through `Envelope` metadata.
- **CI/CD**: Azure DevOps via `azure-pipelines.yaml` + shared
  `hmcts/cpp-azure-devops-templates`. PR = `context-verify`. CI build =
  `context-validation`. SonarQube project
  `uk.gov.moj.cpp.staging.prosecutors.spi:stagingprosecutorsspi`. `dev/release-*`
  branches excluded.
- **Quality gate**: SonarQube — coverage thresholds, duplication, smells
  enforced in CI; no local equivalent at build time.

## Development Workflow & Quality Gates

- **Contract files** (RAML, JSON schemas, CJSE XSD, both
  `subscriptions-descriptor.yaml`, `public-publications-descriptor.yaml`,
  `event-sources.yaml`) MUST be updated **before** the matching Java change,
  and codegen re-run (Principle I + VI).
- The build loop (Principle IV) repeats until `code-reviewer`, `qa`, and
  `spec-validator` each return PASS / COMPLIANT.
- TDD (Principle VIII) MUST be visible in commit history — the failing test
  commit precedes (or is paired with) the production code that satisfies it.
- Every feature built via spec-kit lives under `specs/<JIRA-ID>-slug/`
  (or `specs/NNN-slug/` if not Jira-tracked) containing at least `spec.md`,
  `plan.md`, and `tasks.md`. Flow:
  `/speckit-specify → /speckit-plan → /speckit-tasks → /speckit-implement
  → /speckit-analyze`.
- Required commands run cleanly before merge:
  - `mvn clean install` — full build + codegen + unit tests, green.
  - `./runIntegrationTests.sh` — Dockerised IT run, green (when changes touch
    handlers / listeners / processors / converters / schemas / filter rules).
  - SonarQube quality gate in CI — passing.
- Commit style: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`,
  `refactor:`).
- Pull requests: the description MUST state which principle(s) the change
  touches. Any deviation from a principle requires explicit written
  justification in the PR description and MUST be flagged in the plan's
  "Complexity Tracking" section.
- Branch naming: Jira-prefixed (`DD-XXXXX-feature-slug`) — the speckit
  `before_specify` hook auto-creates these via `/speckit-git-feature`.

## Governance

This constitution supersedes the informal conventions in `.claude/rules/`.
Where this document and those files disagree, this document wins; the rule files
are retained as quick-reference material and MUST be kept in sync.

**Amendment procedure**:

1. Propose the change in a feature spec under `specs/`.
2. Bump `Version` per semantic versioning:
   - **MAJOR** — a breaking principle change, removal, or redefinition that
     invalidates existing practice.
   - **MINOR** — a new principle, new section, or materially expanded guidance.
   - **PATCH** — clarifications, wording, typo fixes, or non-semantic
     refinements.
3. Re-run `/speckit-analyze` on every in-flight feature spec to verify it still
   aligns with the amended principles; update or waive as required.

**Compliance expectations**:

- All PRs MUST honour these principles.
- Deviations MUST be explicitly justified in the PR description and, where
  relevant, in the plan's "Complexity Tracking" table.
- Reviewers MUST block merges that silently violate a NON-NEGOTIABLE principle
  without a written waiver.

**Version**: 1.0.0 | **Ratified**: 2026-06-02 | **Last Amended**: 2026-06-02
