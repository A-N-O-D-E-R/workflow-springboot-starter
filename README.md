# Workflow Spring Boot Starter

Spring Boot starter for OpenEvolve Workflow Engine with auto-configuration support.

## Overview

This starter provides seamless integration of the OpenEvolve Workflow Engine into Spring Boot applications with zero-configuration defaults and flexible customization options.

## Requirements

- Java 17+
- Spring Boot 3.2.0+

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>workflow-spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Quick Start

The starter works out-of-the-box with JPA storage:

```yaml
# application.yml - minimal configuration
spring:
  datasource:
    url: jdbc:h2:mem:workflowdb
    driver-class-name: org.h2.Driver
```

**See the [example application](./example) for a complete working demo.**

## Configuration

### Minimal config :
```yaml
workflow:
  storage:
    type: memory # One Storage 
```

If there is no other config will fall back on default config : 
- Default SlaQueueManager : do nothing
- Default EventHandler : do nothing 
- Default WorkflowComponantFactory : will look for the @component name is equal to compName in workflow definition

### Storage Options

**JPA Storage** (default):
```yaml
workflow:
  storage:
    type: jpa
  jpa:
    enabled: true
    entity-manager-factory-ref: entityManagerFactory
```

**In-Memory Storage** (for testing):
```yaml
workflow:
  storage:
    type: memory
```

**File-Based Storage**:
```yaml
workflow:
  storage:
    type: file
    file-path: ./workflow-data
```

### Component Configuration

```yaml
workflow:
  sla:
    enabled: true           # Enable SLA queue management
  event:
    enabled: true           # Enable event handlers
  factory:
    enabled: true           # Enable component factory
```

## Custom Components

### Custom Event Handler

```java
@Component
@WorkflowEventHandler
public class CustomEventHandler implements EventHandler {
    @Override
    public void handleEvent(WorkflowEvent event) {
        // Your event handling logic
    }
}
```

### Custom Component Factory

```java
@Component
@WorkflowComponentFactory
public class CustomComponentFactory implements WorkflowComponantFactory {
    @Override
    public Object getObject(WorkflowContext ctx) {
        // Your factory logic
    }
}
```

### Custom SLA Queue Manager

```java
@Component
@SlaQueueManagerComponent
public class CustomSlaQueueManager implements SlaQueueManager {
    // Your SLA management logic
}
```

## Features

- Auto-configuration for workflow engine components
- Multiple storage backends (JPA, Memory, File)
- Flexible event handling system
- SLA monitoring support
- Custom component factory support
- Spring Boot configuration properties support

## Building

```bash
mvn clean install
```

## Testing

```bash
mvn test
```

## Example Application

A complete working example is available in the [example](./example) directory. The example demonstrates:

- Basic workflow task execution
- REST API integration
- In-memory storage configuration
- Order processing workflow

To run the example:

```bash
# Build and install the starter
mvn clean install

# Run the example application
cd example
mvn spring-boot:run
```

## License

See LICENSE file for details.
