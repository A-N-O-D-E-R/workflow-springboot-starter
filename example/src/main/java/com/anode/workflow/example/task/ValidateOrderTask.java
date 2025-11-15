package com.anode.workflow.example.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.example.model.Order;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Example workflow task that validates an order.
 *
 * Automatically registered with the workflow engine via @Task annotation.
 */
@Task("validateOrderTask") // Auto-discovered by TaskScanner
@AllArgsConstructor
public class ValidateOrderTask implements InvokableTask {

    private static final Logger logger = LoggerFactory.getLogger(ValidateOrderTask.class);

    private Object context;

    @Override
    public TaskResponse executeStep() {
        if (context instanceof Order) {
            Order order = (Order) context;
            logger.info("Validating order: {}", order.getOrderId());

            // Simple validation logic
            if (order.getAmount() != null && order.getAmount() > 0) {
                order.setStatus("VALIDATED");
                logger.info("Order {} validated successfully", order.getOrderId());
                return new TaskResponse(StepResponseType.OK_PROCEED, null,  ".");
            } else {
                order.setStatus("INVALID");
                logger.warn("Order {} validation failed", order.getOrderId());
                return new TaskResponse(StepResponseType.ERROR_PEND, null,  ".");
            }
        }
        return new TaskResponse(StepResponseType.OK_PROCEED, null,  ".");
    }
}
