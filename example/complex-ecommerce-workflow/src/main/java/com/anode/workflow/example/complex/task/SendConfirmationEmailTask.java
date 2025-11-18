package com.anode.workflow.example.complex.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.complex.model.OrderRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Task("sendconfirmationemailtask")
@AllArgsConstructor
public class SendConfirmationEmailTask implements InvokableTask {

    private final Object context;

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing SendConfirmationEmailTask");

        if (context instanceof OrderRequest order) {
            log.info("Sending confirmation email to: {}", order.getCustomerEmail());
            log.info("Order ID: {}", order.getOrderId());
            log.info("Order Amount: {}", order.getTotalAmount());
            log.info("Confirmation email sent successfully");
        }

        return new TaskResponse(StepResponseType.OK_PROCEED, null, ".");
    }
}
