# Getting Started with Workflow Spring Boot Starter

This guide will walk you through creating your first Spring Boot application with the Workflow Engine.

## Prerequisites

- Java 17 or higher
- Maven 3.6+ or Gradle 7+
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code)
- PostgreSQL 12+ (for production) or use in-memory storage for quick start

## Step 1: Create a Spring Boot Project

Using Spring Initializer (https://start.spring.io/):

- **Project:** Maven
- **Language:** Java
- **Spring Boot:** 3.2.0 or later
- **Group:** com.example
- **Artifact:** workflow-demo
- **Dependencies:**
  - Spring Web
  - Spring Data JPA
  - PostgreSQL Driver (or H2 for quick start)

Or use Spring CLI:
```bash
spring init --dependencies=web,data-jpa,postgresql workflow-demo
cd workflow-demo
```

## Step 2: Add Workflow Starter Dependency

Edit `pom.xml`:

```xml
<dependencies>
    <!-- Existing dependencies... -->

    <!-- Workflow Spring Boot Starter -->
    <dependency>
        <groupId>com.anode</groupId>
        <artifactId>workflow-spring-boot-starter</artifactId>
        <version>0.0.1</version>
    </dependency>
</dependencies>
```

## Step 3: Install Workflow Engine to Local Maven

First, build and install the workflow engine:

```bash
cd ~/Documents/Code/perso/OpenEvolve/workflow
mvn clean install
```

## Step 4: Configure Application

Create `src/main/resources/application.yml`:

### Option A: In-Memory (Quick Start)

```yaml
spring:
  application:
    name: workflow-demo

workflow:
  enabled: true
  storage:
    type: memory
```

### Option B: PostgreSQL (Production)

```yaml
spring:
  application:
    name: workflow-demo

  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_demo
    username: workflow_user
    password: your_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

workflow:
  enabled: true
  storage:
    type: jpa
  jpa:
    enabled: true
    auto-create-schema: true
```

### Database Setup (for PostgreSQL option)

```sql
CREATE DATABASE workflow_demo;
CREATE USER workflow_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE workflow_demo TO workflow_user;
```

## Step 5: Create Your First Workflow

### 5.1 Create Workflow Definition

Create `src/main/java/com/example/workflowdemo/workflow/SampleWorkflow.java`:

```java
package com.example.workflowdemo.workflow;

import com.anode.workflow.entities.steps.Step;
import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import org.springframework.stereotype.Component;

@Component
public class SampleWorkflow {

    public WorkflowDefinition createSimpleWorkflow() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setName("simple-approval-workflow");

        // Create tasks
        Task validateTask = new Task(
            "validate",
            "validateTask",
            "approve",
            null
        );

        Task approveTask = new Task(
            "approve",
            "approveTask",
            "notify",
            null
        );

        Task notifyTask = new Task(
            "notify",
            "notifyTask",
            null,  // End of workflow
            null
        );

        // Add steps to workflow
        workflow.addStep(validateTask);
        workflow.addStep(approveTask);
        workflow.addStep(notifyTask);

        return workflow;
    }
}
```

### 5.2 Create Workflow Tasks

Create `src/main/java/com/example/workflowdemo/tasks/ValidateTask.java`:

```java
package com.example.workflowdemo.tasks;

import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.InvokableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("validateTask")
public class ValidateTask implements InvokableTask {

    private static final Logger logger = LoggerFactory.getLogger(ValidateTask.class);

    @Override
    public TaskResponse executeTask(WorkflowContext context, Task task) {
        logger.info("Validating request for case: {}", context.getCaseId());

        // Your validation logic here
        boolean isValid = true;

        if (isValid) {
            return TaskResponse.success(task.getNext(), "Validation successful");
        } else {
            return TaskResponse.error("VALIDATION_FAILED", "Validation failed");
        }
    }
}
```

Create `src/main/java/com/example/workflowdemo/tasks/ApproveTask.java`:

```java
package com.example.workflowdemo.tasks;

import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.InvokableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("approveTask")
public class ApproveTask implements InvokableTask {

    private static final Logger logger = LoggerFactory.getLogger(ApproveTask.class);

    @Override
    public TaskResponse executeTask(WorkflowContext context, Task task) {
        logger.info("Approving request for case: {}", context.getCaseId());

        // Your approval logic here
        // In real scenario, this might pause for human approval

        return TaskResponse.success(task.getNext(), "Approved");
    }
}
```

Create `src/main/java/com/example/workflowdemo/tasks/NotifyTask.java`:

```java
package com.example.workflowdemo.tasks;

import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.InvokableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("notifyTask")
public class NotifyTask implements InvokableTask {

    private static final Logger logger = LoggerFactory.getLogger(NotifyTask.class);

    @Override
    public TaskResponse executeTask(WorkflowContext context, Task task) {
        logger.info("Sending notification for case: {}", context.getCaseId());

        // Your notification logic here (email, SMS, etc.)

        return TaskResponse.success(null, "Notification sent");
    }
}
```

### 5.3 Create Component Factory

Create `src/main/java/com/example/workflowdemo/config/WorkflowComponentFactory.java`:

```java
package com.example.workflowdemo.config;

import com.anode.workflow.service.InvokableRoute;
import com.anode.workflow.service.InvokableTask;
import com.anode.workflow.service.WorkflowComponantFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class WorkflowComponentFactory implements WorkflowComponantFactory {

    private final ApplicationContext context;

    public WorkflowComponentFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public InvokableTask getTask(String componentName) {
        try {
            return context.getBean(componentName, InvokableTask.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown task component: " + componentName, e);
        }
    }

    @Override
    public InvokableRoute getRoute(String componentName) {
        try {
            return context.getBean(componentName, InvokableRoute.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown route component: " + componentName, e);
        }
    }
}
```

## Step 6: Create REST Controller

Create `src/main/java/com/example/workflowdemo/controller/WorkflowController.java`:

```java
package com.example.workflowdemo.controller;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.service.runtime.RuntimeService;
import com.example.workflowdemo.workflow.SampleWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private SampleWorkflow sampleWorkflow;

    @PostMapping("/start/{caseId}")
    public WorkflowResponse startWorkflow(@PathVariable String caseId) {
        WorkflowDefinition workflow = sampleWorkflow.createSimpleWorkflow();

        WorkflowVariables variables = new WorkflowVariables();
        // Add any initial variables if needed

        WorkflowContext context = runtimeService.startCase(
            caseId,
            workflow,
            variables,
            null
        );

        return new WorkflowResponse(
            caseId,
            context.isCaseCompleted(),
            "Workflow started successfully"
        );
    }

    @PostMapping("/resume/{caseId}")
    public WorkflowResponse resumeWorkflow(@PathVariable String caseId) {
        runtimeService.resumeCase(caseId);

        return new WorkflowResponse(
            caseId,
            true,
            "Workflow resumed successfully"
        );
    }

    // Response DTO
    public static class WorkflowResponse {
        private String caseId;
        private boolean completed;
        private String message;

        public WorkflowResponse(String caseId, boolean completed, String message) {
            this.caseId = caseId;
            this.completed = completed;
            this.message = message;
        }

        // Getters and setters
        public String getCaseId() { return caseId; }
        public boolean isCompleted() { return completed; }
        public String getMessage() { return message; }
    }
}
```

## Step 7: Run the Application

```bash
mvn spring-boot:run
```

You should see in the logs:
```
Workflow Engine Auto-Configuration enabled
  Storage Type: MEMORY
  JPA Enabled: false
  SLA Enabled: false
Creating WorkflowService bean
Creating RuntimeService bean
```

## Step 8: Test the Workflow

Using curl:

```bash
# Start a workflow
curl -X POST http://localhost:8080/api/workflow/start/CASE-001

# Response:
# {
#   "caseId": "CASE-001",
#   "completed": true,
#   "message": "Workflow started successfully"
# }
```

Check the logs - you should see:
```
Validating request for case: CASE-001
Approving request for case: CASE-001
Sending notification for case: CASE-001
```

## Next Steps

1. **Add More Complex Workflows** - Include routes, branches, joins
2. **Implement Persistence** - Switch to JPA with PostgreSQL
3. **Add SLA Monitoring** - Implement SlaQueueManager
4. **Create Custom Event Handlers** - Track workflow events
5. **Add Security** - Integrate with Spring Security
6. **Create UI** - Build a frontend for workflow management

## Common Issues

### Issue: Bean creation errors

**Solution:** Make sure `WorkflowComponentFactory` is annotated with `@Component` and can be found by component scan.

### Issue: Tasks not found

**Solution:** Ensure all task components are annotated with `@Component("taskName")` and the bean name matches the workflow definition.

### Issue: Database connection failed

**Solution:** Verify PostgreSQL is running and credentials are correct in `application.yml`.

## Additional Resources

- [Main README](README.md) - Complete configuration reference
- [Workflow Engine Documentation](../workflow/docs/) - Core engine documentation
- [Example Applications](examples/) - More complete examples

---

Congratulations! You've created your first Spring Boot application with the Workflow Engine. 🎉
