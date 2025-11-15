package com.anode.workflow.example.complex.service;

import com.anode.workflow.example.complex.model.OrderRequest;
import com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service orchestrating the complex e-commerce workflow.
 *
 * <p>This workflow demonstrates:
 * <ul>
 *   <li>10+ tasks with complex business logic</li>
 *   <li>Conditional routing based on customer type</li>
 *   <li>Variable passing between tasks</li>
 *   <li>Sequential task execution</li>
 *   <li>Error handling with validation</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplexOrderWorkflowService {

    private final FluentWorkflowBuilderFactory workflowFactory;

    /**
     * Executes the complete order processing workflow.
     *
     * <p>Workflow Steps:
     * <ol>
     *   <li>Validate Order - Basic validation</li>
     *   <li>Customer Type Route - Conditional routing</li>
     *   <li>Apply Discount - For VIP/Corporate customers</li>
     *   <li>Check Inventory - Verify stock availability</li>
     *   <li>Reserve Inventory - Reserve items</li>
     *   <li>Calculate Shipping - Compute shipping costs</li>
     *   <li>Process Payment - Charge customer</li>
     *   <li>Update Inventory - Deduct stock</li>
     *   <li>Notify Warehouse - Alert warehouse team</li>
     *   <li>Arrange Shipping - Create shipping labels</li>
     *   <li>Send Confirmation - Email customer</li>
     * </ol>
     */
    public void processComplexOrder(OrderRequest order) {
        log.info("================================================");
        log.info("Starting complex workflow for order: {}", order.getOrderId());
        log.info("Customer: {} ({})", order.getCustomerId(), order.getCustomerType());
        log.info("Items: {}, Total: {}", order.getItems().size(), order.getTotalAmount());
        log.info("================================================");

        workflowFactory.builder(order.getOrderId())
            // Step 1: Validate order data
            .task("validateordertask")

            // Step 2: Route based on customer type
            // This demonstrates conditional routing
            .task("customertyperoute")

            // Step 3: Apply discounts (for VIP/Corporate)
            // May be skipped for regular customers via route
            .task("applydiscounttask")

            // Step 4: Check inventory availability
            .task("checkinventorytask")

            // Step 5: Reserve inventory items
            .task("reserveinventorytask")

            // Step 6: Calculate shipping costs
            .task("calculateshippingtask")

            // Step 7: Process payment
            .task("processpaymenttask")

            // Step 8: Update inventory levels
            .task("updateinventorytask")

            // Step 9: Notify warehouse for fulfillment
            .task("notifywarehousetask")

            // Step 10: Arrange shipping
            .task("arrangeshippingtask")

            // Step 11: Send confirmation email
            .task("sendconfirmationemailtask")

            // Add workflow variables
            .variable("order", order)
            .variable("workflowStartTime", System.currentTimeMillis())

            // Execute the workflow
            .start();

        log.info("================================================");
        log.info("Complex workflow completed for order: {}", order.getOrderId());
        log.info("================================================");
    }
}
