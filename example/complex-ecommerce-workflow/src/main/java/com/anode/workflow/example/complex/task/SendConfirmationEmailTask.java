package com.anode.workflow.example.complex.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.complex.model.OrderRequest;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Task
public class SendConfirmationEmailTask implements InvokableTask {

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing SendConfirmationEmailTask");

        OrderRequest order = (OrderRequest) getWorkflowContext().getVariables().getValue("order");
        BigDecimal finalAmount = (BigDecimal) getWorkflowContext().getVariables().getValue("finalAmount");
        String trackingNumber = (String) getWorkflowContext().getVariables().getValue("trackingNumber");

        log.info("Sending confirmation email to: {}", order.getCustomerEmail());
        log.info("Order ID: {}", order.getOrderId());
        log.info("Final Amount: {}", finalAmount);
        log.info("Tracking Number: {}", trackingNumber);

        return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
    }
}
