# workflow-spring-boot-starter

Spring Boot 4 integration of the [`workflow`](https://github.com/A-N-O-D-E-R/workflow) engine:
engines declared in configuration, tasks as Spring beans, durable PostgreSQL storage, and a
`WorkflowEngine` facade to start, resume and reopen cases.

- Java 25, Spring Boot 4.1, `io.github.a-n-o-d-e-r:workflow` 0.1.
- License: Apache 2.0.

## Installation

```xml
<dependency>
    <groupId>io.github.a-n-o-d-e-r</groupId>
    <artifactId>workflow-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
<!-- for the JDBC storage -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

## Quick start

Tasks are beans annotated with `@Task` (a `@Component`). Implement `ContextualTask` when the task
needs to know which case it runs for — the singleton bean is then safely shared by concurrent cases:

```java
@Task("approval")
class Approval implements ContextualTask {
    @Override
    public TaskResponse execute(WorkflowContext context) {
        return isApproved(context.getCaseId())
                ? new TaskResponse(StepResponseType.OK_PROCEED, "", "")
                : new TaskResponse(StepResponseType.OK_PEND, "", "approvers"); // waits in a work basket
    }
}
```

`InvokableTask` (no context) is accepted too. Routes are `InvokableRoute` or `ContextualRoute` beans.

```yaml
workflow:
  engines:
    - name: main
      storage:
        type: jdbc
        jdbc:
          initialize-schema: true   # or manage the DDL with Liquibase/Flyway
```

```java
@Autowired WorkflowEngine engine;

engine.startWorkflow("order-42", "main", List.of("prepare", "approval"), Map.of("customer", "c-1"));
// ... later, from any instance, after the approval happened:
engine.resumeWorkflow("order-42", "main", null);
```

Each step of a definition built from bean names is named after its bean.

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `workflow.runtime.threads` | `10` | Threads running parallel route branches (0 = calling thread) |
| `workflow.runtime.idle-timeout` | `30s` | Idle time before a branch thread is released |
| `workflow.runtime.key-separator` | `_` | Separator in persistence keys |
| `workflow.runtime.error-workbasket` | `workflow_error` | Work basket of a case whose step threw |
| `workflow.engines[n].name` | — | Engine name; the first engine is the default |
| `workflow.engines[n].storage.type` | `memory` | `jdbc`, `memory`, `file` or `custom` |
| `workflow.engines[n].storage.jdbc.document-table` | `workflow_document` | JSON documents table |
| `workflow.engines[n].storage.jdbc.counter-table` | `workflow_counter` | Counters table |
| `workflow.engines[n].storage.jdbc.initialize-schema` | `false` | Create both tables at startup |
| `workflow.engines[n].storage.file-path` | `./workflow-data` | Directory of the `file` storage |
| `workflow.engines[n].storage.custom-bean-name` | — | `CommonService` bean of the `custom` storage |
| `workflow.engines[n].sla.enabled` | `false` | Pass SLA milestones to an `SlaQueueManager` |

### Storage

- **jdbc** — durable, for production. Every persisted object (definition, case state, SLA, audit
  log) is a JSON document (`WorkflowJson` of the engine) in PostgreSQL; any instance can resume any
  case after a restart. DDL: `classpath:com/anode/workflow/spring/storage/schema-postgresql.sql`.
- **file** — one JSON file per key; development only.
- **memory** — lost on restart; tests and demos.
- **custom** — your own `CommonService` bean.

### Custom components

`@WorkflowComponentFactory`, `@WorkflowEventHandler` and `@SlaQueueManagerComponent` are
`@Component`s: annotate your `WorkflowComponantFactory`, `EventHandler` or `SlaQueueManager` and it
replaces the default (bean lookup by step component name, no-op handler, no-op SLA manager).

## Concurrency

A `RuntimeService` carries the state of the case it operates on, so `WorkflowEngine` uses a new one
for every operation (see `WorkflowRuntimes`). Operations on **different** cases can run
concurrently; concurrent operations on the **same** case must be serialized by the caller, for
example with a row lock in the caller's transaction.

## Build

```bash
mvn verify      # JDK 25; the JDBC tests start PostgreSQL with Testcontainers (Docker required)
```

Releases: `mvn release:prepare` creates a `v<version>` tag; GitHub Actions publishes to Maven Central.
