package com.anode.workflow.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Workflow Example Application
 *
 * This example demonstrates:
 * - Order processing workflow with multiple tasks
 * - Conditional routing based on shipping method
 * - Event handling for audit logging
 * - SLA milestone tracking
 * - REST API for workflow operations
 * - Multiple storage backend options
 */
@SpringBootApplication
public class WorkflowExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowExampleApplication.class, args);
    }
}
