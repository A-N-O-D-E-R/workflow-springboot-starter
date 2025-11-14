# Workflow Example Application

A simple example demonstrating the Workflow Spring Boot Starter.

## Overview

This example application demonstrates:
- Basic workflow task execution
- Spring Boot integration with the workflow starter
- In-memory storage configuration
- REST API for triggering workflows

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

## Testing the Workflow

### Create an Order (Valid)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "amount": 99.99
  }'
```

Expected response:
```json
{
  "orderId": "generated-uuid",
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

Expected response:
```json
{
  "orderId": "generated-uuid",
  "customerName": "Jane Smith",
  "amount": 0,
  "status": "INVALID"
}
```

### Health Check

```bash
curl http://localhost:8080/api/orders/health
```

## Workflow Flow

The application processes orders through a simple workflow:

1. **ValidateOrderTask** - Validates the order (checks if amount > 0)
2. **ProcessPaymentTask** - Processes payment (only if validated)

## Configuration

The example uses in-memory storage (`workflow.storage.type=memory`) for simplicity. See `application.yml` for full configuration.

## Project Structure

```
example/
├── src/main/java/com/anode/workflow/example/
│   ├── WorkflowExampleApplication.java   # Main Spring Boot application
│   ├── controller/
│   │   └── OrderController.java          # REST controller
│   ├── service/
│   │   └── OrderWorkflowService.java     # Workflow orchestration
│   ├── task/
│   │   ├── ValidateOrderTask.java        # Validation workflow task
│   │   └── ProcessPaymentTask.java       # Payment workflow task
│   └── model/
│       └── Order.java                    # Order domain model
└── src/main/resources/
    └── application.yml                   # Application configuration
```

## Next Steps

To extend this example:
- Add more workflow tasks
- Implement custom event handlers
- Add JPA storage with database persistence
- Implement SLA monitoring
- Add custom component factory

See the main README in the parent directory for more configuration options.
