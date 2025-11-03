# Workflow Spring Boot Starter

Spring Boot starter for the OpenEvolve Workflow Engine, providing auto-configuration and seamless integration with Spring Boot applications.

## Features

- ✅ **Auto-configuration** - Zero-configuration setup for most use cases
- ✅ **Multiple storage backends** - JPA, in-memory, file-based, or custom
- ✅ **Spring Integration** - Native Spring Boot beans and dependency injection
- ✅ **Type-safe configuration** - Full IDE autocomplete support via `application.properties`
- ✅ **Production-ready** - Transaction management, connection pooling, and error handling
- ✅ **Extensible** - Easy to customize with your own beans

## Quick Start

### 1. Add Dependency

Add the starter to your `pom.xml`:

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>workflow-spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

### 2. Configure (Optional)

Add to `application.yml`:

```yaml
workflow:
  enabled: true
  storage:
    type: jpa  # or memory, file, custom
```

### 3. Use in Your Code

```java
@Service
public class OrderService {

    @Autowired
    private RuntimeService runtimeService;

    public void processOrder(String orderId) {
        WorkflowContext context = runtimeService.startCase(
            orderId,
            workflowDefinition,
            variables,
            null
        );
    }
}
```

That's it! The workflow engine is now fully integrated with your Spring Boot application.

## Configuration

### Storage Configuration

#### JPA Storage (Production)

```yaml
workflow:
  storage:
    type: jpa
  jpa:
    enabled: true
    entity-manager-factory-ref: entityManagerFactory
    auto-create-schema: false
```

**Prerequisites:**
- Spring Data JPA dependency
- Database configuration (PostgreSQL, MySQL, etc.)
- EntityManagerFactory bean configured

**Database Setup:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_db
    username: workflow_user
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

#### Memory Storage (Development/Testing)

```yaml
workflow:
  storage:
    type: memory
```

**⚠️ WARNING:** All data is lost when the application restarts.

#### File Storage (Development)

```yaml
workflow:
  storage:
    type: file
    file-path: ./workflow-data
```

**⚠️ WARNING:** Not suitable for production use.

#### Custom Storage

```yaml
workflow:
  storage:
    type: custom
    custom-bean-name: myCustomStorage
```

Then provide your own bean:

```java
@Bean
public CommonService myCustomStorage() {
    return new MyCustomStorageImpl();
}
```

### Event Handler Configuration

#### Default Event Handler

```yaml
workflow:
  event-handler:
    enabled: true
```

#### Custom Event Handler

```java
@Component
public class MyEventHandler implements EventHandler {
    @Override
    public void onWorkflowStart(String caseId) {
        // Your logic here
    }

    @Override
    public void onWorkflowComplete(String caseId) {
        // Your logic here
    }

    // Implement other methods...
}
```

### SLA Configuration

```yaml
workflow:
  sla:
    enabled: true
    queue-manager-bean: mySlaQueueManager
```

Provide your SLA queue manager:

```java
@Component
public class MySlaQueueManager implements SlaQueueManager {
    @Override
    public void scheduleMilestone(String caseId, String milestoneName, long dueTimeMillis) {
        // Schedule milestone check
    }

    @Override
    public void cancelMilestone(String caseId, String milestoneName) {
        // Cancel milestone
    }
}
```

## Complete Configuration Reference

```yaml
workflow:
  # Enable or disable auto-configuration
  enabled: true

  storage:
    # Storage type: jpa, memory, file, custom
    type: jpa
    # File path for file-based storage
    file-path: ./workflow-data
    # Custom storage bean name
    custom-bean-name: myCustomStorage

  jpa:
    # Enable JPA storage
    enabled: true
    # EntityManagerFactory bean reference
    entity-manager-factory-ref: entityManagerFactory
    # Auto-create database schema
    auto-create-schema: false

  event-handler:
    # Enable default event handler
    enabled: true
    # Custom event handler bean name
    bean-name: myEventHandler

  sla:
    # Enable SLA queue manager
    enabled: false
    # Custom SLA queue manager bean name
    queue-manager-bean: mySlaQueueManager
```

## Advanced Usage

### Custom Component Factory

Create custom workflow components:

```java
@Component
public class MyComponentFactory implements WorkflowComponantFactory {
    @Override
    public InvokableTask getTask(String componentName) {
        return switch (componentName) {
            case "sendEmail" -> new SendEmailTask();
            case "validateOrder" -> new ValidateOrderTask();
            default -> throw new IllegalArgumentException("Unknown task: " + componentName);
        };
    }

    @Override
    public InvokableRoute getRoute(String componentName) {
        return switch (componentName) {
            case "approvalDecision" -> new ApprovalDecisionRoute();
            default -> throw new IllegalArgumentException("Unknown route: " + componentName);
        };
    }
}
```

### Transaction Management

The starter automatically integrates with Spring's transaction management:

```java
@Service
@Transactional
public class WorkflowService {

    @Autowired
    private RuntimeService runtimeService;

    public void executeWorkflow(String caseId) {
        // All workflow operations are automatically transactional
        WorkflowContext context = runtimeService.startCase(...);
        runtimeService.resumeCase(caseId);
    }
}
```

### Multi-Database Support

If you need workflow data in a separate database:

```java
@Configuration
public class WorkflowDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "workflow.datasource")
    public DataSource workflowDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean workflowEntityManagerFactory(
            @Qualifier("workflowDataSource") DataSource dataSource) {
        // Configure entity manager factory
    }
}
```

Then configure:

```yaml
workflow:
  jpa:
    entity-manager-factory-ref: workflowEntityManagerFactory
  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_db
    username: workflow_user
    password: workflow_pass
```

## Example Applications

### Simple Order Processing

```java
@SpringBootApplication
public class OrderProcessingApp {
    public static void main(String[] args) {
        SpringApplication.run(OrderProcessingApp.class, args);
    }
}

@Service
public class OrderWorkflowService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private WorkflowDefinitionRepository workflowRepo;

    public void processNewOrder(Order order) {
        // Load workflow definition
        WorkflowDefinition orderWorkflow = workflowRepo.findByName("orderProcessing");

        // Set variables
        WorkflowVariables variables = new WorkflowVariables();
        variables.set("orderId", order.getId());
        variables.set("amount", order.getTotalAmount());
        variables.set("customerId", order.getCustomerId());

        // Start workflow
        WorkflowContext context = runtimeService.startCase(
            order.getId(),
            orderWorkflow,
            variables,
            null
        );

        logger.info("Started order processing workflow for order: {}", order.getId());
    }

    public void resumeOrder(String orderId) {
        runtimeService.resumeCase(orderId);
    }
}
```

### With Custom Tasks

```java
@Component
public class SendEmailTask implements InvokableTask {

    @Autowired
    private EmailService emailService;

    @Override
    public TaskResponse executeTask(WorkflowContext context, Task task) {
        String recipientEmail = context.getVariable("customerEmail");
        String orderId = context.getVariable("orderId");

        emailService.sendOrderConfirmation(recipientEmail, orderId);

        return TaskResponse.success("emailSent", "Email sent successfully");
    }
}
```

## Testing

### Unit Tests

```java
@SpringBootTest
class WorkflowServiceTest {

    @Autowired
    private RuntimeService runtimeService;

    @Test
    void testOrderWorkflow() {
        // Use in-memory storage for tests
        WorkflowDefinition workflow = createTestWorkflow();
        WorkflowVariables vars = new WorkflowVariables();

        WorkflowContext context = runtimeService.startCase(
            "TEST-001",
            workflow,
            vars,
            null
        );

        assertThat(context.isCaseCompleted()).isTrue();
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class WorkflowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword());
    }

    @Test
    void testWorkflowPersistence() {
        // Test with real database
    }
}
```

### Test Configuration

For tests, use memory storage:

```yaml
# application-test.yml
workflow:
  storage:
    type: memory
```

## Troubleshooting

### Auto-configuration not working

1. Check that starter is in dependencies
2. Verify `workflow.enabled=true` in configuration
3. Enable debug logging:
   ```yaml
   logging:
     level:
       com.anode.workflow: DEBUG
   ```

### Database connection issues

1. Verify database is running
2. Check connection URL, username, password
3. Ensure database user has proper permissions
4. Check if schema exists (or enable auto-create)

### Bean not found errors

If you get "No qualifying bean" errors:

1. Ensure auto-configuration is enabled
2. Check component scanning includes workflow packages
3. Verify bean creation order (use `@DependsOn` if needed)

## Performance Tuning

### Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

### JPA Optimization

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
```

## Migration Guide

### From Manual Configuration

Before:
```java
@Configuration
public class WorkflowConfig {
    @Bean
    public RuntimeService runtimeService() {
        CommonService dao = new MemoryDao();
        WorkflowComponantFactory factory = new MyComponentFactory();
        return WorkflowService.instance().getRunTimeService(dao, factory, null, null);
    }
}
```

After:
```java
// Just add the starter dependency - auto-configuration handles everything!

@Component
public class MyComponentFactory implements WorkflowComponantFactory {
    // Your implementation
}
```

## Support

- [Workflow Engine Documentation](../workflow/docs/)
- [GitHub Issues](https://github.com/your-org/workflow-springboot-starter/issues)
- [Spring Boot Reference](https://spring.io/projects/spring-boot)

## License

Same as the workflow engine project.

---

**Version:** 0.0.1
**Compatible with:** Spring Boot 3.x, Java 17+
**Workflow Engine Version:** 0.0.1
