package com.anode.workflow.example.workflow;

import com.anode.workflow.entities.steps.Branch;
import com.anode.workflow.entities.steps.Route;
import com.anode.workflow.entities.steps.Step;
import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Workflow definition for order processing
 *
 * This workflow demonstrates:
 * - Sequential task execution
 * - Conditional routing
 * - Error handling
 * - Complete order lifecycle
 */
@Component
public class OrderProcessingWorkflow {

    public WorkflowDefinition createOrderWorkflow() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setName("order-processing-workflow");

        // Step 1: Validate Order
        Task validateOrderTask = new Task(
                "validate-order",
                "validateOrderTask",
                "process-payment",
                null
        );

        // Step 2: Process Payment
        Task processPaymentTask = new Task(
                "process-payment",
                "processPaymentTask",
                "check-inventory",
                null
        );

        // Step 3: Check Inventory
        Task checkInventoryTask = new Task(
                "check-inventory",
                "checkInventoryTask",
                "shipping-route",  // Route to determine shipping method
                null
        );

        // Step 4: Shipping Method Route (conditional)
        // Define branches for different shipping methods
        Map<String, Branch> shippingBranches = new HashMap<>();
        shippingBranches.put("STANDARD", new Branch("STANDARD", "prepare-shipment"));
        shippingBranches.put("EXPRESS", new Branch("EXPRESS", "prepare-express-shipment"));
        shippingBranches.put("OVERNIGHT", new Branch("OVERNIGHT", "prepare-overnight-shipment"));

        Route shippingMethodRoute = new Route(
                "shipping-route",
                "shippingMethodRoute",
                null,
                shippingBranches,
                Step.StepType.S_ROUTE
        );

        // Step 5a: Prepare Standard Shipment
        Task prepareShipmentTask = new Task(
                "prepare-shipment",
                "prepareShipmentTask",
                "send-notification",
                null
        );

        // Step 5b: Prepare Express Shipment (same implementation for demo)
        Task prepareExpressShipmentTask = new Task(
                "prepare-express-shipment",
                "prepareShipmentTask",  // reuse same implementation
                "send-notification",
                null
        );

        // Step 5c: Prepare Overnight Shipment (same implementation for demo)
        Task prepareOvernightShipmentTask = new Task(
                "prepare-overnight-shipment",
                "prepareShipmentTask",  // reuse same implementation
                "send-notification",
                null
        );

        // Step 6: Send Notification (end of workflow)
        Task sendNotificationTask = new Task(
                "send-notification",
                "sendNotificationTask",
                null,  // End of workflow
                null
        );

        // Add all steps to workflow
        workflow.addStep(validateOrderTask);
        workflow.addStep(processPaymentTask);
        workflow.addStep(checkInventoryTask);
        workflow.addStep(shippingMethodRoute);
        workflow.addStep(prepareShipmentTask);
        workflow.addStep(prepareExpressShipmentTask);
        workflow.addStep(prepareOvernightShipmentTask);
        workflow.addStep(sendNotificationTask);

        return workflow;
    }
}
