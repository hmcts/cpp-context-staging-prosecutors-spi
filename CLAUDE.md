# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **cpp-context-staging-prosecutors-spi**, a Java-based microservices system implementing CQRS (Command Query Responsibility Segregation) with Event Sourcing for the UK Ministry of Justice criminal prosecution case processing. The system receives prosecution cases from the CJSE (Criminal Justice Services Exchange) system, filters and transforms them, and publishes them to the Common Platform Prosecution Case File system.

**Architecture Pattern**: Event-Driven CQRS + Event Sourcing using the Justice Services Framework
**Build System**: Maven multi-module project (12 modules)
**Deployment**: Java EE WAR files (Wildfly/JBoss) + Azure Functions

## Build Commands

```bash
# Build entire project
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Run unit tests only
mvn test

# Run integration tests (requires Docker environment)
./runIntegrationTests.sh

# Build specific module
mvn clean install -pl stagingprosecutorsspi-command-api

# Package Azure Functions
cd stagingprosecutors-azure-functions && mvn clean package
```

## Testing

```bash
# Run unit tests for all modules
mvn test

# Run integration tests for specific module
mvn clean verify -Pstagingprosecutorsspi-integration-test

# Run integration tests with Docker (full environment)
# Requires CPP_DOCKER_DIR environment variable pointing to cpp-developers-docker repo
export CPP_DOCKER_DIR=/path/to/cpp-developers-docker
./runIntegrationTests.sh
```

## System Commands (JMX Management)

The system supports JMX-based administrative commands via the Justice Services Framework:

```bash
# List all available system commands
./runSystemCommand.sh

# Run a specific command (e.g., CATCHUP for event replay)
./runSystemCommand.sh CATCHUP

# View help for command client
./runSystemCommand.sh --help
```

Common system commands include:
- **CATCHUP**: Replays events to rebuild read models from event store
- **REBUILD**: Forces complete rebuild of view stores from events
- **VALIDATE_CATALOG**: Validates JSON schema catalog integrity

## Module Architecture

The system follows a strict CQRS layered architecture:

### Command Side (Write)
- **stagingprosecutorsspi-command-api** (WAR): REST/SOAP endpoints for receiving prosecution cases
  - Handlers: `hmcts.cjs.receive-spi-message`, `hmcts.cjs.resend-message`
- **stagingprosecutorsspi-command-handler** (WAR): Business logic and event sourcing persistence
  - Commands: ReceiveProsecutionCase, FilterProsecutionCase, UpdatePoliceSystem, SendResult

### Query Side (Read)
- **stagingprosecutorsspi-query-api** (WAR): Query endpoints for retrieving case data
  - Handler: `stagingprosecutorsspi.query.cpp-message`
- **stagingprosecutorsspi-query-view** (WAR): Read model views from denormalized viewstore

### Event Processing
- **stagingprosecutorsspi-event-listener** (WAR): Subscribes to domain events and updates viewstore
  - Events: prosecution-case-received, spi-result-prepared-for-sending, spi-oi-police-system-updated
- **stagingprosecutorsspi-event-processor** (WAR): Complex event transformations and workflow orchestration
  - Uses Activiti BPMN workflow engine for case processing
  - 20+ processor classes for data conversion and filtering
  - Publishes events to Prosecution Case File system

### Domain Layer
- **stagingprosecutorsspi-domain**: Parent module containing:
  - **domain-aggregates**: Core aggregate roots (SPIPoliceCase, CPPMessage, OIMessage, CJSEMessage)
  - **domain-events**: Event definitions (generated from JSON schemas)
  - **domain-value-schema**: Value objects and schema definitions
  - **domain-transformation**: Data transformation utilities and anonymization

### Supporting Modules
- **stagingprosecutorsspi-cjse-schema** (JAR): JAXB/JAXWS code generation from CJSE XSD schemas
- **stagingprosecutorsspi-validation-rules** (JAR): CJSE message validation logic
- **stagingprosecutorsspi-viewstore**: Persistence layer with Liquibase migrations and Deltaspike repositories
- **stagingprosecutorsspi-event-sources** (JAR): Event source descriptors (YAML) defining JMS/REST URIs
- **stagingprosecutorsspi-healthchecks** (JAR): Service health monitoring endpoints
- **stagingprosecutors-azure-functions** (JAR): Azure Functions for serverless case filtering (16+ functions)

## Event Flow

### High-Level Overview

```
External System (CJSE) → Command API → Command Handler → Event Store
                                                            ↓
                                                        Event Topic (JMS)
                                                            ↓
                                        ┌──────────────────┴──────────────────┐
                                        ↓                                     ↓
                                Event Listener                        Event Processor
                                        ↓                                     ↓
                                    Viewstore                         Complex Transformations
                                        ↑                                     ↓
                                   Query View                     Prosecution Case File API
                                        ↑                                     ↓
                                    Query API                          Public Event Topic
                                                                             ↓
                                                                    Azure Functions (Filtering)
```

### Detailed Event Publishing and Consumption Flow

This system uses **event-store version 17.101.5** which implements a multi-stage asynchronous publishing pipeline.

#### Stage 1: Command Processing and Event Storage

```
CJSE System (SOAP/REST)
    ↓
Command API: hmcts.cjs.receive-spi-message handler
    ↓
Command Handler: Executes business logic (e.g., ReceiveProsecutionCaseHandler)
    ↓
Aggregate: SPIPoliceCase.apply() creates domain events
    ↓
Event Store: EventStreamManager.append()
    ↓
PublishingEventAppender stores events
    ↓
[event_log table] + [pre_publish_queue table] (PostgreSQL)
```

**Key point:** Command completes and returns to CJSE immediately. Event publishing happens asynchronously.

#### Stage 2: Event Numbering (Background Process)

```
PrePublisherTimerBean (EJB Timer - runs every 500ms)
    ↓
Processes events from pre_publish_queue
    ↓
Calculates event_number and previous_event_number
    ↓
[published_event table] + [publish_queue table]
```

**Purpose:** Assigns global event numbers for ordering and gap detection without blocking command execution.

#### Stage 3: Publishing to JMS Topic

```
PublisherTimerBean (EJB Timer - runs every 500ms)
    ↓
Processes events from publish_queue
    ↓
JmsEventPublisher.publish()
    ↓
Resolves JMS destination from event name:
  - Event: stagingprosecutorsspi.event.prosecution-case-received
  - Destination: jms:topic:stagingprosecutorsspi.event
    ↓
📡 JMS Topic: stagingprosecutorsspi.event
```

**Configuration:** Event sources defined in `stagingprosecutorsspi-event-sources/src/yaml/event-sources.yaml`:

```yaml
event_sources:
  - name: stagingprosecutorsspi.event.source
    is_default: true
    location:
      jms_uri: jms:topic:stagingprosecutorsspi.event
      rest_uri: http://localhost:8080/stagingprosecutorsspi-service/event-source-api/rest
      data_source: java:/app/stagingprosecutorsspi-service/DS.eventstore
```

#### Stage 4: Event Consumption via @Handles

**Event Processor Example:**

`stagingprosecutorsspi-event-processor/src/main/java/.../SpiProsecutionCaseProcessor.java:51-64`

```java
@ServiceComponent(EVENT_PROCESSOR)
public class SpiProsecutionCaseProcessor {

    @Handles("stagingprosecutorsspi.event.prosecution-case-received")
    public void onSpiProsecutionCaseReceived(final Envelope<SpiProsecutionCaseReceived> envelope) {
        // 1. Extract prosecution case from envelope
        // 2. Convert SPI format to PCF (Prosecution Case File) format
        // 3. Send command to Prosecution Case File system
        sender.sendAsAdmin(envelopeFrom(metadata, payload));
    }

    @Handles("public.prosecutioncasefile.prosecution-case-unsupported")
    public void onCaseUnsupportedMessageReceived(final Envelope<PublicProsecutionCaseUnsupported> envelope) {
        // Handle error cases from downstream system
    }
}
```

**How @Handles Works:**
- Justice Services Framework subscribes to JMS topics automatically
- Framework deserializes JSON events into typed POJOs
- Routes events to methods annotated with `@Handles` based on event name
- Wraps events in `Envelope` with metadata (event_number, stream_id, timestamps)
- No manual JMS listener code required

**Event Listener Example:**

`stagingprosecutorsspi-event-listener` subscribes to same topic but updates viewstore:

```java
@ServiceComponent(EVENT_LISTENER)
public class SpiEventListener {

    @Handles("stagingprosecutorsspi.event.prosecution-case-received")
    public void handleProsecutionCaseReceived(Envelope<SpiProsecutionCaseReceived> event) {
        // Update denormalized view in viewstore database
        cppMessageRepository.save(...);
    }
}
```

#### Complete Flow with Timings

```
T+0ms:   CJSE sends prosecution case → Command API
T+50ms:  Command Handler stores event → event_log + pre_publish_queue
T+50ms:  ✅ Response returned to CJSE (fast synchronous path)
         ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
T+500ms: PrePublisherTimer wakes up → assigns event_number
         → Stores in published_event + publish_queue
T+1000ms: PublisherTimer wakes up → publishes to JMS topic
T+1001ms: Event Processor receives event → transforms to PCF format
          → Sends to Prosecution Case File API
T+1001ms: Event Listener receives event → updates viewstore
```

### Event Ordering and Reliability

**Event Buffer:** When events arrive out-of-order from JMS (common in distributed systems), the `ConsecutiveEventBufferService` holds later events until missing ones arrive, then releases them in correct sequence order.

**Event Tracking:** `ProcessedEventTrackingService` records which events each component has processed, enabling automatic gap detection and catchup.

**Retry Logic:** Failed event processing triggers automatic retry via `@Retry` interceptor with exponential backoff.

### Key Database Tables

**Event Store (event-store framework):**
- `event_log` - Raw domain events
- `pre_publish_queue` - Events awaiting numbering
- `published_event` - Events with event_number metadata
- `publish_queue` - Events ready for JMS

**SPI Application:**
- `event_buffer.stream_buffer` - Out-of-order event buffering
- `event_buffer.processed_event` - Event processing tracking
- `viewstore.cpp_message` - Denormalized query view

## Key Technologies

- **Framework**: Justice Services Framework (UK MOJ), CDI, Deltaspike
- **Event Sourcing**: Custom event store with JMS (JBoss Topic messaging)
- **Web/API**: JAX-RS, JAX-WS, JAXB for REST/SOAP
- **Workflow**: Activiti BPMN engine for case processing orchestration
- **Persistence**: Deltaspike persistence, Liquibase migrations, PostgreSQL
- **Cloud**: Azure Functions, Azure Storage (Tables/Blobs), Azure Event Grid
- **Testing**: JUnit 5, Mockito, Wiremock, Awaitility, XMLUnit, JSONAssert
- **Code Generation**: Multiple Maven plugins generate code from JSON schemas, XSDs, and YAML descriptors

## Code Generation

The project extensively uses schema-driven code generation:

### POJO Generation from JSON Schemas
- **Location**: JSON schemas in `src/main/resources/json/` directories
- **Plugin**: `pojo-generation-plugin`
- **Generated**: Request/response POJOs, event classes with annotations
- **Type Mappings**: UUID → java.util.UUID, date-time → java.time.ZonedDateTime

### JAXB Generation from XSD
- **Module**: stagingprosecutorsspi-cjse-schema
- **Plugin**: `maven-jaxb2-plugin`
- **Purpose**: CJSE XML schema bindings for SOAP/XML processing

### Messaging and REST Clients
- **Plugins**: `messaging-adapter-generator-plugin`, `rest-client-generator-plugin`
- **Source**: YAML descriptors in `src/yaml/` directories
- **Generated**: Messaging handlers, REST client interfaces

**Important**: Always regenerate code after schema changes by running `mvn clean install` on the affected module.

## Event Sources Configuration

Event sources are defined in `stagingprosecutorsspi-event-sources/src/yaml/event-sources.yaml`:

```yaml
event_sources:
  - name: stagingprosecutorsspi.event.source
    is_default: true
    location:
      jms_uri: jms:topic:stagingprosecutorsspi.event
      rest_uri: http://localhost:8080/stagingprosecutorsspi-service/event-source-api/rest
      data_source: java:/app/stagingprosecutorsspi-service/DS.eventstore

  - name: public.event.source
    location:
      jms_uri: jms:topic:public.event
```

## Core Aggregates and Commands

### SPIPoliceCase Aggregate
- **States**: LIVE, EJECTED
- **Commands**:
  - `receivePoliceCase`: Initial case receipt from CJSE
  - `filterProsecutionCase`: Apply filtering rules
  - `handleEjectCase`: Eject invalid cases

### CPPMessage Aggregate
- **Commands**:
  - `prepareCPPMessageForSending`: Prepare message for CJSE transmission
  - `resendMessage`: Retry failed message sending

### OIMessage Aggregate (Operational Interface)
- **Commands**:
  - `oiRequestReceived`: Receive OI request
  - `updatePoliceSystemId`: Update police system reference
  - `prepareOIResponseForXSDFailures`: Handle schema validation failures

## Azure Functions

The `stagingprosecutors-azure-functions` module contains 16+ HTTP-triggered Azure Functions for case filtering:

- **ApplyFilterRules**: Main case filtering logic based on court centre, prosecutor code, case initiation type
- **CaseEjectedOrFilteredStatus**: Query case filtering status
- **NotifyCourtStore**, **RelayCaseOnCPP**, **RelayCaseToLibra**: Case routing functions
- **CJSEMetaDataFunction**, **CheckProsecutorIsLiveFunction**: Metadata and validation functions

**Deployment**: Package creates ZIP in `target/zip/` for Azure deployment with Java 11+ runtime.

## External Service Dependencies

The system integrates with multiple external services:

- **Prosecution Case File API**: Publishes filtered cases
- **Reference Data API**: Lookups for court codes, offence codes, etc.
- **ID Mapper Service**: Cross-system identifier mapping
- **System Users Service**: User authentication and authorization
- **Azure Storage**: Case filtering tables and blob storage
- **CJSE/CPS Systems**: Criminal justice case exchange via SOAP/REST

## Development Notes

### Running Individual Modules

Each WAR module can be deployed independently to Wildfly/JBoss. The `stagingprosecutorsspi-service` module is a composite WAR that bundles all components for single-deployment environments.

### Database Migrations

Liquibase changesets are in `stagingprosecutorsspi-viewstore/stagingprosecutorsspi-viewstore-liquibase/src/main/resources/liquibase/`. The integration test script runs migrations automatically:
- Event log (event store)
- Event log aggregate snapshot
- Event buffer
- Viewstore (CPPMessage entity)
- System tables
- Event tracking

### Adding New Commands

1. Define command JSON schema in appropriate domain module under `src/main/resources/json/schema/`
2. Run `mvn clean install` to generate POJO
3. Create handler in command-handler module annotated with `@Handles`
4. Update aggregate in domain-aggregates to handle command
5. Emit domain events from aggregate
6. Create event listener in event-listener module if viewstore update needed

### Adding New Queries

1. Define query request/response schemas in query-api module
2. Generate POJOs via `mvn clean install`
3. Create query handler in query-view module annotated with `@Handles`
4. Query viewstore persistence repositories

### Adding New Events

1. Define event JSON schema in domain-events module under `src/main/resources/json/schema/`
2. Ensure schema has `"x-event-name"` extension for event name
3. Run `mvn clean install` to generate event POJO with `@Event` annotation
4. Create event listener in event-listener module or event processor in event-processor module

## Troubleshooting

### Code Generation Not Working
- Ensure JSON schemas are valid and in correct `src/main/resources/json/` location
- Run `mvn clean install` on parent module first, then specific module
- Check Maven output for plugin execution errors

### Integration Tests Failing
- Verify `CPP_DOCKER_DIR` environment variable is set
- Ensure Docker is running and cpp-developers-docker containers are available
- Check Wildfly logs in Docker containers for deployment errors

### Event Replay (CATCHUP) Issues
- Verify event store database connection
- Check event source configuration in event-sources.yaml
- Use JMX system commands to manually trigger CATCHUP

### Azure Functions Deployment Issues
- Ensure Java 11+ runtime configured in Azure Function App
- Set `WEBSITE_RUN_FROM_PACKAGE=1` configuration
- Check application insights for runtime errors

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
