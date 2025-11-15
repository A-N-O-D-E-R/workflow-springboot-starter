package com.anode.workflow.example.service;

import com.anode.workflow.example.model.Order;
import com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilderFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service that processes orders using workflow engine with fluent API.
 *
 * Demonstrates:
 * - Fluent workflow builder usage
 * - Task execution with @Task annotation
 * - Multiple engine support
 */
@Service
@RequiredArgsConstructor
public class OrderWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(OrderWorkflowService.class);

    private final FluentWorkflowBuilderFactory workflowFactory;

    /**
     * Process an order through the workflow.
     *
     * Workflow steps:
     * 1. ValidateOrderTask - Validates order amount > 0
     * 2. ProcessPaymentTask - Processes payment if validated
     *
     * @param order the order to process
     * @return the processed order with updated status
     */
    public Order processOrder(Order order) {
        // Generate order ID if not present
        if (order.getOrderId() == null) {
            order.setOrderId(UUID.randomUUID().toString());
        }

        logger.info("Starting workflow for order: {}", order.getOrderId());

        // Execute workflow using fluent builder API
        workflowFactory.builder(order.getOrderId())
            .engine("order-engine")              // Select the order processing engine
            .task("validateordertask")           // Task 1: Validate order
            .task("processpaymenttask")          // Task 2: Process payment
            .variable("order", order)            // Pass order as workflow variable
            .start();                            // Execute the workflow

        logger.info("Workflow completed for order: {} with status: {}",
            order.getOrderId(), order.getStatus());

        return order;
    }
}
