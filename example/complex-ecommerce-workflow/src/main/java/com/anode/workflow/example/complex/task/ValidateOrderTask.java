package com.anode.workflow.example.complex.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.complex.model.OrderRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates order data, customer information, and business rules.
 */
@Slf4j
@Task("validateordertask")
@AllArgsConstructor
public class ValidateOrderTask implements InvokableTask {

    private final Object context;

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing ValidateOrderTask");

        if (context instanceof OrderRequest order) {
            log.info("Validating order: {}", order.getOrderId());
            log.info("Customer: {} (Type: {})", order.getCustomerId(), order.getCustomerType());
            log.info("Total amount: {}", order.getTotalAmount());
            log.info("Items count: {}", order.getItems().size());

            // Validation logic
            if (order.getTotalAmount().doubleValue() < 0) {
                log.error("Invalid order amount: {}", order.getTotalAmount());
                return new TaskResponse(StepResponseType.ERROR_PEND, "invalidAmount", ".");
            }

            if (order.getItems().isEmpty()) {
                log.error("Order has no items");
                return new TaskResponse(StepResponseType.ERROR_PEND, "noItems", ".");
            }

            log.info("Order validation successful");
        }

        return new TaskResponse(StepResponseType.OK_PROCEED, null, ".");
    }
}
