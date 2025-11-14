package com.anode.workflow.example.service;

import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.workflows.WorkflowVariable.WorkflowVariableType;
import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.task.ProcessPaymentTask;
import com.anode.workflow.example.task.ValidateOrderTask;
import com.anode.workflow.service.runtime.RuntimeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service that processes orders using workflow tasks.
 */
@Service
@RequiredArgsConstructor
public class OrderWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(OrderWorkflowService.class);

    private final List<RuntimeService> services;


    /**
     * Process an order through the workflow.
     *
     * @param order the order to process
     * @return the processed order
     */
    public Order processOrder(Order order) {
        // Generate order ID if not present
        if (order.getOrderId() == null) {
            order.setOrderId(UUID.randomUUID().toString());
        }
        
        // Get the first (and only) runtime service configured for the order engine
        RuntimeService rs = services.stream().findFirst().orElseThrow(() -> new RuntimeException("No runtime service configured"));
        WorkflowDefinition def = new WorkflowDefinition();
        def.addStep(new Task("validate", "validateOrderTask", "payement", null)); //Task(String name, String componentName, String next, String userData) {
        def.addStep(new Task("payement", "processPaymentTask", null,null));
        WorkflowVariables vars = new WorkflowVariables();
        vars.setValue("order", WorkflowVariableType.OBJECT, order);
        rs.startCase(order.getOrderId(), def, vars, null);
        
        return order;
    }
}
