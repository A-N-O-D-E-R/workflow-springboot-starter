# Workflow Example Application - E-Commerce Order Processing

This is a comprehensive example application demonstrating the use of the **workflow-spring-boot-starter** for building a complete e-commerce order processing system using workflow orchestration.

## Table of Contents

- [Overview](#overview)
- [Features Demonstrated](#features-demonstrated)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Running the Application](#running-the-application)
- [API Usage](#api-usage)
- [Workflow Details](#workflow-details)
- [Testing](#testing)
- [Configuration Profiles](#configuration-profiles)
- [Project Structure](#project-structure)

## Overview

This example application implements a complete order processing workflow with the following stages:

1. **Order Validation** - Validates order data
2. **Payment Processing** - Processes payment through payment gateway
3. **Inventory Check** - Verifies and reserves inventory
4. **Shipping Route** - Conditional routing based on shipping method
5. **Shipment Preparation** - Prepares shipment and generates tracking
6. **Notification** - Sends customer notifications

The application showcases **Test-Driven Development (TDD)** with comprehensive test coverage for all workflow components.

## Features Demonstrated

### Core Workflow Features
- ✅ **Sequential Task Execution** - Tasks execute in defined order
- ✅ **Conditional Routing** - Dynamic branching based on shipping method
- ✅ **Error Handling** - Graceful error handling at each step
- ✅ **State Management** - Order state tracked throughout workflow
- ✅ **Context Variables** - Data sharing between workflow steps

### Spring Boot Integration
- ✅ **Auto-Configuration** - Zero-configuration workflow setup
- ✅ **Component Factory** - Spring-based task/route resolution
- ✅ **Dependency Injection** - Full DI support in tasks and routes
- ✅ **REST API** - Complete REST endpoints for workflow operations

### Advanced Features
- ✅ **Event Handling** - Audit logging with custom event handler
- ✅ **SLA Monitoring** - Milestone tracking for deadlines
- ✅ **Multiple Storage Backends** - Memory, File, JPA options
- ✅ **Profile-Based Configuration** - Dev, Test, Prod profiles

### TDD Approach
- ✅ **Comprehensive Tests** - 100% test coverage for workflow tasks
- ✅ **Unit Tests** - All tasks, routes, and components tested
- ✅ **Integration Tests** - End-to-end workflow validation
- ✅ **Mock Services** - Realistic service implementations

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     REST API Layer                          │
│              (OrderController)                              │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                  Workflow Engine                            │
│            (RuntimeService)                                 │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Tasks      │  │   Routes     │  │   Events     │
│              │  │              │  │              │
│ - Validate   │  │ - Shipping   │  │ - Audit      │
│ - Payment    │  │   Method     │  │   Handler    │
│ - Inventory  │  │              │  │              │
│ - Shipment   │  │              │  │ - SLA        │
│ - Notify     │  │              │  │   Manager    │
└──────┬───────┘  └──────────────┘  └──────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│              Business Services Layer                        │
│  (Payment, Inventory, Shipping, Notification)              │
└─────────────────────────────────────────────────────────────┘
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (for production profile) or use in-memory storage

### Build the Workflow Starter

First, build and install the workflow starter:

```bash
cd ..
mvn clean install
```

### Build the Example Application

```bash
cd example
mvn clean install
```

## Running the Application

### Quick Start (In-Memory Storage)

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080` with in-memory storage.

### Development Mode (File Storage)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production Mode (PostgreSQL)

1. Set up PostgreSQL database:

```sql
CREATE DATABASE workflow_db;
CREATE USER workflow_user WITH PASSWORD 'workflow_password';
GRANT ALL PRIVILEGES ON DATABASE workflow_db TO workflow_user;
```

2. Run application:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Test Mode (H2 Database)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

H2 Console available at: `http://localhost:8080/h2-console`

## API Usage

### Submit a New Order

**Endpoint:** `POST /api/orders`

**Request Body:** (see `sample-requests/create-order-standard.json`)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @sample-requests/create-order-standard.json
```

**Response:**
```json
{
  "orderId": "ORD-2025-001",
  "status": "COMPLETED",
  "workflowCompleted": true,
  "message": "Order submitted successfully"
}
```

### Get Order Status

**Endpoint:** `GET /api/orders/{orderId}`

```bash
curl http://localhost:8080/api/orders/ORD-2025-001
```

### Resume Workflow (For Paused Workflows)

**Endpoint:** `POST /api/orders/{orderId}/resume`

```bash
curl -X POST http://localhost:8080/api/orders/ORD-2025-001/resume
```

### Cancel Order

**Endpoint:** `DELETE /api/orders/{orderId}`

```bash
curl -X DELETE http://localhost:8080/api/orders/ORD-2025-001
```

## Workflow Details

### Order Processing Workflow

The workflow consists of the following steps:

1. **Validate Order** (`validateOrderTask`)
   - Validates order ID, customer info, email format
   - Validates items, payment info, shipping info
   - Validates order total > 0
   - Status: CREATED → VALIDATED

2. **Process Payment** (`processPaymentTask`)
   - Validates payment amount matches order total
   - Processes payment through payment gateway
   - Generates transaction ID
   - Status: VALIDATED → PAYMENT_COMPLETED

3. **Check Inventory** (`checkInventoryTask`)
   - Checks inventory availability
   - Reserves inventory for order
   - Status: PAYMENT_COMPLETED → INVENTORY_CHECKED

4. **Shipping Method Route** (`shippingMethodRoute`)
   - Conditional routing based on shipping method:
     - STANDARD → `prepare-shipment`
     - EXPRESS → `prepare-express-shipment`
     - OVERNIGHT → `prepare-overnight-shipment`

5. **Prepare Shipment** (`prepareShipmentTask`)
   - Creates shipment with carrier
   - Generates tracking number
   - Calculates estimated delivery
   - Status: INVENTORY_CHECKED → SHIPPED

6. **Send Notification** (`sendNotificationTask`)
   - Sends shipping notification email
   - Sends order completion email
   - Status: SHIPPED → COMPLETED

### Workflow Diagram

```
START
  │
  ▼
┌──────────────────┐
│ Validate Order   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Process Payment  │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Check Inventory  │
└────────┬─────────┘
         │
         ▼
   ┌─────────┐
   │ Shipping│
   │  Route  │
   └────┬────┘
        │
   ┌────┼────┬────────────┐
   │    │    │            │
   ▼    ▼    ▼            ▼
Standard Express Overnight
   │    │    │            │
   └────┼────┴────────────┘
        │
        ▼
┌──────────────────┐
│ Prepare Shipment │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Send Notification│
└────────┬─────────┘
         │
         ▼
        END
```

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=ValidateOrderTaskTest
```

### Test Coverage

The example includes comprehensive tests:

- **Task Tests** - 5 test classes with 30+ test cases
- **Route Tests** - Conditional logic testing
- **Component Factory Tests** - Bean resolution testing
- **Integration Tests** - End-to-end workflow testing

### TDD Approach

This example follows strict TDD principles:

1. ✅ **Tests Written First** - All tests created before implementation
2. ✅ **Red-Green-Refactor** - Tests fail, then pass, then refactor
3. ✅ **High Coverage** - Every method and branch tested
4. ✅ **Clear Test Names** - Tests document expected behavior

## Configuration Profiles

### Default (In-Memory)

```yaml
workflow:
  storage:
    type: memory
```

Best for: Quick testing, development

### Dev (File Storage)

```yaml
workflow:
  storage:
    type: file
    file-path: ./workflow-data
```

Best for: Local development with persistence

### Prod (PostgreSQL)

```yaml
workflow:
  storage:
    type: jpa
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_db
```

Best for: Production deployment

### Test (H2)

```yaml
workflow:
  storage:
    type: jpa
spring:
  datasource:
    url: jdbc:h2:mem:testdb
```

Best for: Integration testing

## Project Structure

```
example/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/anode/workflow/example/
│   │   │       ├── config/               # Configuration
│   │   │       │   ├── AuditEventHandler.java
│   │   │       │   ├── OrderSlaQueueManager.java
│   │   │       │   └── WorkflowComponentFactory.java
│   │   │       ├── controller/           # REST API
│   │   │       │   ├── OrderController.java
│   │   │       │   └── OrderRequest.java
│   │   │       ├── model/                # Domain Models
│   │   │       │   ├── Order.java
│   │   │       │   ├── OrderItem.java
│   │   │       │   ├── OrderStatus.java
│   │   │       │   ├── PaymentInfo.java
│   │   │       │   ├── ShippingInfo.java
│   │   │       │   └── ShippingMethod.java
│   │   │       ├── routes/               # Workflow Routes
│   │   │       │   └── ShippingMethodRoute.java
│   │   │       ├── service/              # Service Interfaces
│   │   │       │   ├── InventoryService.java
│   │   │       │   ├── NotificationService.java
│   │   │       │   ├── OrderRepository.java
│   │   │       │   ├── PaymentService.java
│   │   │       │   ├── ShippingService.java
│   │   │       │   └── impl/             # Mock Implementations
│   │   │       │       ├── InMemoryOrderRepository.java
│   │   │       │       ├── MockInventoryService.java
│   │   │       │       ├── MockNotificationService.java
│   │   │       │       ├── MockPaymentService.java
│   │   │       │       └── MockShippingService.java
│   │   │       ├── tasks/                # Workflow Tasks
│   │   │       │   ├── CheckInventoryTask.java
│   │   │       │   ├── PrepareShipmentTask.java
│   │   │       │   ├── ProcessPaymentTask.java
│   │   │       │   ├── SendNotificationTask.java
│   │   │       │   └── ValidateOrderTask.java
│   │   │       ├── workflow/             # Workflow Definitions
│   │   │       │   └── OrderProcessingWorkflow.java
│   │   │       └── WorkflowExampleApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-docker.yml
│   └── test/
│       └── java/
│           └── com/anode/workflow/example/
│               ├── config/
│               │   └── WorkflowComponentFactoryTest.java
│               ├── routes/
│               │   └── ShippingMethodRouteTest.java
│               └── tasks/
│                   ├── CheckInventoryTaskTest.java
│                   ├── PrepareShipmentTaskTest.java
│                   ├── ProcessPaymentTaskTest.java
│                   ├── SendNotificationTaskTest.java
│                   └── ValidateOrderTaskTest.java
├── sample-requests/                      # Sample API Requests
│   ├── create-order-standard.json
│   ├── create-order-express.json
│   └── create-order-overnight.json
├── pom.xml
└── README.md
```

## Key Components

### Tasks

- **ValidateOrderTask** - Validates order data before processing
- **ProcessPaymentTask** - Handles payment processing
- **CheckInventoryTask** - Verifies and reserves inventory
- **PrepareShipmentTask** - Creates shipment and tracking
- **SendNotificationTask** - Sends customer notifications

### Routes

- **ShippingMethodRoute** - Conditional routing based on shipping method

### Configuration

- **WorkflowComponentFactory** - Resolves workflow components from Spring context
- **AuditEventHandler** - Logs workflow events for auditing
- **OrderSlaQueueManager** - Tracks SLA milestones and deadlines

### Services

All services have mock implementations for demonstration:

- **PaymentService** - Payment processing simulation
- **InventoryService** - Inventory management with in-memory tracking
- **ShippingService** - Shipment creation and tracking
- **NotificationService** - Email notification simulation
- **OrderRepository** - In-memory order storage

## Best Practices Demonstrated

1. **Separation of Concerns** - Clear separation between workflow, business logic, and infrastructure
2. **Dependency Injection** - Full use of Spring DI in all components
3. **Error Handling** - Comprehensive error handling at each workflow step
4. **Logging** - Structured logging throughout the application
5. **Configuration Management** - Profile-based configuration for different environments
6. **Testing** - TDD with comprehensive test coverage
7. **Documentation** - Clear code documentation and README

## Next Steps

To extend this example:

1. **Add Database Persistence** - Replace mock services with real JPA repositories
2. **Add Authentication** - Secure REST endpoints with Spring Security
3. **Add Async Processing** - Make long-running tasks asynchronous
4. **Add Metrics** - Integrate Spring Boot Actuator for monitoring
5. **Add Message Queue** - Use RabbitMQ/Kafka for event publishing
6. **Add Retries** - Implement retry logic for failed tasks
7. **Add Compensation** - Add compensation tasks for rollback scenarios

## License

This example application is provided as-is for demonstration purposes.

## Support

For questions or issues, please refer to the main workflow-spring-boot-starter documentation.
