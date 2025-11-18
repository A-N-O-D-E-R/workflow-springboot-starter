package com.anode.workflow.example.complex;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.example.complex.task.*;
import com.anode.workflow.service.WorkflowComponantFactory;
import com.anode.workflow.spring.autoconfigure.annotations.WorkflowComponentFactory;

/**
 * Custom component factory for the complex e-commerce workflow.
 *
 * This factory is responsible for instantiating workflow tasks
 * with the necessary dependencies injected from the workflow context.
 */
@WorkflowComponentFactory
public class ComponentFactory implements WorkflowComponantFactory {

    @Override
    public Object getObject(WorkflowContext ctx) {
        // Retrieve the order from workflow variables
        Object order = ctx.getProcessVariables().getObject("order");

        String componentName = ctx.getCompName();

        // Route to appropriate task based on component name
        return switch (componentName) {
            case "validateordertask" -> new ValidateOrderTask(order);
            case "applydiscounttask" -> new ApplyDiscountTask(order);
            case "checkinventorytask" -> new CheckInventoryTask(order);
            case "reserveinventorytask" -> new ReserveInventoryTask(order);
            case "calculateshippingtask" -> new CalculateShippingTask(order);
            case "processpaymenttask" -> new ProcessPaymentTask(order);
            case "updateinventorytask" -> new UpdateInventoryTask(order);
            case "notifywarehousetask" -> new NotifyWarehouseTask(order);
            case "arrangeshippingtask" -> new ArrangeShippingTask(order);
            case "sendconfirmationemailtask" -> new SendConfirmationEmailTask(order);
            default -> null;
        };
    }
}
