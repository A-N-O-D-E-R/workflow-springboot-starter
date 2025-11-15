package com.anode.workflow.example.complex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Complex E-Commerce Workflow Example Application.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>10+ tasks in a single workflow</li>
 *   <li>Conditional routing based on customer type</li>
 *   <li>Complex business logic (discounts, shipping, payment)</li>
 *   <li>Variable passing between tasks</li>
 *   <li>Error handling and validation</li>
 * </ul>
 *
 * <p>To run:
 * <pre>
 * mvn spring-boot:run
 * </pre>
 *
 * <p>Test endpoints:
 * <pre>
 * curl -X POST http://localhost:8080/api/orders/sample/vip
 * curl -X POST http://localhost:8080/api/orders/sample/corporate
 * curl -X POST http://localhost:8080/api/orders/sample/international
 * </pre>
 */
@SpringBootApplication
public class ComplexWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComplexWorkflowApplication.class, args);
    }
}
