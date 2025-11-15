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
public class ProcessPaymentTask implements InvokableTask {

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing ProcessPaymentTask");

        OrderRequest order = (OrderRequest) getWorkflowContext().getVariables().getValue("order");
        BigDecimal finalAmount = (BigDecimal) getWorkflowContext().getVariables().getValue("finalAmount");

        log.info("Processing payment for order: {}", order.getOrderId());
        log.info("Payment method: {}", order.getPaymentMethod());
        log.info("Amount to charge: {}", finalAmount);

        // Simulate payment processing
        boolean paymentSuccess = simulatePaymentProcessing(order.getPaymentMethod());

        if (!paymentSuccess) {
            log.error("Payment failed for order: {}", order.getOrderId());
            return new TaskResponse(StepResponseType.FAILED, "paymentFailed", null);
        }

        getWorkflowContext().getVariables().setValue("paymentStatus", "SUCCESS");
        getWorkflowContext().getVariables().setValue("transactionId", "TXN-" + System.currentTimeMillis());

        log.info("Payment processed successfully");

        return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
    }

    private boolean simulatePaymentProcessing(OrderRequest.PaymentMethod method) {
        // Simulate 95% success rate
        return Math.random() > 0.05;
    }
}
