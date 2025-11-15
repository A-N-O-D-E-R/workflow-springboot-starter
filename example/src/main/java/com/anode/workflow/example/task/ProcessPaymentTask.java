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
 * Example workflow task that processes payment for an order.
 *
 * Automatically registered with the workflow engine via @Task annotation.
 * Task name: "processpaymenttask" (lowercase class name by default)
 */
@Component("processPaymentTask")
@Task  // Auto-discovered by TaskScanner
@AllArgsConstructor
public class ProcessPaymentTask implements InvokableTask {

    private static final Logger logger = LoggerFactory.getLogger(ProcessPaymentTask.class);

    private Object context;

    @Override
    public TaskResponse executeStep() {
        if (context instanceof Order) {
            Order order = (Order) context;
            logger.info("Processing payment for order: {}", order.getOrderId());

            // Simulate payment processing
            if ("VALIDATED".equals(order.getStatus())) {
                order.setStatus("PAYMENT_PROCESSED");
                logger.info("Payment processed for order {}, amount: ${}",
                    order.getOrderId(), order.getAmount());
                return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
            } else {
                logger.error("Cannot process payment for order {} - not validated",
                    order.getOrderId());
                return new TaskResponse(StepResponseType.ERROR_PEND, null, null);
            }
        }
        return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
    }
}
