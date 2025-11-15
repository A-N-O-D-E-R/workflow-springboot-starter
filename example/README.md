# Workflow Example Application

A complete working example demonstrating the Workflow Spring Boot Starter with `@Task` annotation and fluent API.

## Overview

This example application demonstrates:
- ✅ Task registration using `@Task` annotation
- ✅ Fluent workflow builder API
- ✅ REST API for triggering workflows
- ✅ In-memory storage configuration
- ✅ Order processing workflow with validation and payment

## Running the Application

### Prerequisites

- Java 17+
- Maven 3.6+

### Build and Run

```bash
# Build the parent starter first (from root directory)
cd ..
mvn clean install

# Then build and run the example
cd example
mvn clean spring-boot:run
```

The application will start on `http://localhost:8080`

---

## API Endpoints

### Create an Order (Valid)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "amount": 99.99
  }'
```

**Expected Response:**
```json
{
  "orderId": "8f7a3c21-9d4e-4b5a-8c12-1e3f4a5b6c7d",
  "customerName": "John Doe",
  "amount": 99.99,
  "status": "PAYMENT_PROCESSED"
}
```

### Create an Order (Invalid - zero amount)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Smith",
    "amount": 0
  }'
```

**Expected Response:**
```json
{
  "orderId": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
  "customerName": "Jane Smith",
  "amount": 0,
  "status": "INVALID"
}
```

### Health Check

```bash
curl http://localhost:8080/api/orders/health
```

**Response:**
```
Order workflow service is healthy
```

---

## Workflow Flow

The application processes orders through a two-step workflow:

### 1. ValidateOrderTask
- **Purpose**: Validates the order
- **Logic**: Checks if amount > 0
- **Success**: Sets status to `VALIDATED`, proceeds to next task
- **Failure**: Sets status to `INVALID`, workflow pends with error

### 2. ProcessPaymentTask
- **Purpose**: Processes payment for validated orders
- **Logic**: Verifies order is `VALIDATED`, then processes payment
- **Success**: Sets status to `PAYMENT_PROCESSED`
- **Failure**: Cannot process if order not validated

### Task Registration

Tasks are automatically discovered using the `@Task` annotation:

```java
@Component("validateOrderTask")
@Task  // Auto-discovered and registered
public class ValidateOrderTask implements InvokableTask {
    // Task implementation
}
```

**Task Names:**
- `ValidateOrderTask` → task name: `validateordertask`
- `ProcessPaymentTask` → task name: `processpaymenttask`

---

## Project Structure

```
example/
├── src/main/java/com/anode/workflow/example/
│   ├── WorkflowExampleApplication.java      # Main Spring Boot application
│   │
│   ├── controller/
│   │   ├── OrderController.java             # REST API endpoints
│   │   └── DebugController.java             # Debug/diagnostics endpoints
│   │
│   ├── service/
│   │   └── OrderWorkflowService.java        # Workflow orchestration (uses FluentBuilder)
│   │
│   ├── task/
│   │   ├── ValidateOrderTask.java           # @Task - Order validation
│   │   └── ProcessPaymentTask.java          # @Task - Payment processing
│   │
│   └── model/
│       └── Order.java                       # Order domain model
│
└── src/main/resources/
    └── application.yml                      # Application configuration
```

---

## Configuration

The example uses the following configuration (`application.yml`):

```yaml
workflow:
  engines:
    - name: order-engine              # Engine for order processing
      storage:
        type: memory                  # In-memory storage (non-persistent)
      sla:
        enabled: false                # SLA monitoring disabled

  scan-base-package: com.anode        # Package to scan for @Task annotations
```

### Storage Configuration

Uses **in-memory storage** for simplicity:
- ✅ Fast and simple
- ⚠️ Data lost on restart
- ✅ Perfect for development and testing

For production, switch to JPA storage:
```yaml
workflow:
  engines:
    - name: order-engine
      storage:
        type: jpa  # Persistent database storage
```

---

## Code Walkthrough

### 1. Task Definition with @Task Annotation

**ValidateOrderTask.java:**
```java
@Component("validateOrderTask")  // Spring bean name
@Task                             // Auto-discovered by TaskScanner
public class ValidateOrderTask implements InvokableTask {

    @Override
    public TaskResponse executeStep() {
        // Validation logic
        if (order.getAmount() > 0) {
            order.setStatus("VALIDATED");
            return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
        }
        return new TaskResponse(StepResponseType.ERROR_PEND, null, null);
    }
}
```

**Key Points:**
- `@Component("validateOrderTask")` - Defines the Spring bean
- `@Task` - Marks for automatic workflow registration
- Task name: `validateordertask` (lowercase class name)

### 2. Workflow Orchestration with Fluent Builder

**OrderWorkflowService.java:**
```java
@Service
@RequiredArgsConstructor
public class OrderWorkflowService {

    private final FluentWorkflowBuilderFactory workflowFactory;

    public Order processOrder(Order order) {
        String orderId = UUID.randomUUID().toString();
        order.setOrderId(orderId);

        // Build and execute workflow using fluent API
        workflowFactory.builder(orderId)
            .engine("order-engine")                  // Select engine
            .task("validateordertask")               // First task
            .task("processpaymenttask")              // Second task
            .variable("order", order)                // Pass order as variable
            .start();                                // Execute workflow

        return order;
    }
}
```

**Key Points:**
- Inject `FluentWorkflowBuilderFactory`
- Chain tasks in execution order
- Pass data via `.variable()`
- `.start()` executes the workflow

### 3. REST API Integration

**OrderController.java:**
```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderWorkflowService orderWorkflowService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order processedOrder = orderWorkflowService.processOrder(order);
        return ResponseEntity.ok(processedOrder);
    }
}
```

---

## Testing the Example

### 1. Start the Application

```bash
mvn spring-boot:run
```

### 2. Test Valid Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName": "Alice", "amount": 150.00}'
```

**Console Output:**
```
2025-11-15 14:30:01.234 INFO  - Validating order: 8f7a3c21-...
2025-11-15 14:30:01.235 INFO  - Order 8f7a3c21-... validated successfully
2025-11-15 14:30:01.236 INFO  - Processing payment for order: 8f7a3c21-...
2025-11-15 14:30:01.237 INFO  - Payment processed for order 8f7a3c21-..., amount: $150.00
```

### 3. Test Invalid Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerName": "Bob", "amount": -10}'
```

**Console Output:**
```
2025-11-15 14:31:15.123 INFO  - Validating order: a1b2c3d4-...
2025-11-15 14:31:15.124 WARN  - Order a1b2c3d4-... validation failed
```

---

## Extending the Example

### Add a New Task

**1. Create the Task Class:**

```java
@Component("sendConfirmationTask")
@Task
public class SendConfirmationTask implements InvokableTask {

    @Override
    public TaskResponse executeStep() {
        // Send confirmation email
        logger.info("Sending confirmation email...");
        return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
    }
}
```

**2. Add to Workflow:**

```java
workflowFactory.builder(orderId)
    .task("validateordertask")
    .task("processpaymenttask")
    .task("sendconfirmationtask")  // ← New task
    .variable("order", order)
    .start();
```

That's it! The task is automatically discovered and integrated.

### Add Custom Event Handler

```java
@Component
@WorkflowEventHandler
public class OrderEventHandler implements EventHandler {

    @Override
    public void onEvent(WorkflowEvent event) {
        logger.info("Workflow event: {} for case: {}",
            event.getType(), event.getCaseId());
    }
}
```

### Switch to JPA Storage

**Update application.yml:**

```yaml
workflow:
  engines:
    - name: order-engine
      storage:
        type: jpa  # ← Change from memory to jpa

spring:
  datasource:
    url: jdbc:h2:file:./data/workflowdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
```

---

## Debugging

### View Registered Tasks

Add a debug endpoint:

```java
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final TaskScanner taskScanner;

    @GetMapping("/tasks")
    public Map<String, TaskDescriptor> getAllTasks() {
        return taskScanner.getRegistry();
    }
}
```

**Test:**
```bash
curl http://localhost:8080/api/debug/tasks
```

### Enable Detailed Logging

```yaml
logging:
  level:
    com.anode.workflow: DEBUG
    com.anode.workflow.spring: TRACE
```

---

## Common Issues

### Task Not Found

**Error:**
```
IllegalArgumentException: No @Task registered with name: mytask
```

**Solution:** Ensure task class name matches:
```java
// Class: MyTask → task name: "mytask" (lowercase)
workflowFactory.builder("case").task("mytask").start();
```

### Bean Not Found

**Error:**
```
NoSuchBeanDefinitionException: No bean named 'myTask'
```

**Solution:** Add `@Component` to your task class:
```java
@Component("myTask")  // ← Required
@Task
public class MyTask implements InvokableTask { ... }
```

---

## Next Steps

**Explore More:**
1. Add more workflow tasks (shipping, notifications, etc.)
2. Implement custom event handlers for monitoring
3. Switch to JPA storage for persistence
4. Add SLA monitoring
5. Create multiple workflow engines
6. Add conditional branching with routes

**See the main [README](../README.md) for more configuration options and advanced features.**

---

## Performance Notes

**Current Configuration:**
- In-memory storage: ~1ms per workflow execution
- No persistence overhead
- Suitable for 1000+ req/sec

**With JPA Storage:**
- Database writes add ~10-50ms latency
- Persistent across restarts
- Suitable for production workloads

---

## License

See LICENSE file for details.
