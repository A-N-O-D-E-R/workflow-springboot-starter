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
@Task("processpaymenttask")
@AllArgsConstructor
public class ProcessPaymentTask implements InvokableTask {

    private final Object context;

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing ProcessPaymentTask");

        if (context instanceof OrderRequest order) {
            log.info("Processing payment for order: {}", order.getOrderId());
            log.info("Payment method: {}", order.getPaymentMethod());
            log.info("Amount to charge: {}", order.getTotalAmount());

            // Simulate payment processing
            boolean paymentSuccess = simulatePaymentProcessing(order.getPaymentMethod());

            if (!paymentSuccess) {
                log.error("Payment failed for order: {}", order.getOrderId());
                return new TaskResponse(StepResponseType.ERROR_PEND, "paymentFailed", ".");
            }

            String transactionId = "TXN-" + System.currentTimeMillis();
            log.info("Payment processed successfully with transaction ID: {}", transactionId);
        }

        return new TaskResponse(StepResponseType.OK_PROCEED, null, ".");
    }

    private boolean simulatePaymentProcessing(OrderRequest.PaymentMethod method) {
        // Simulate 95% success rate
        return Math.random() > 0.05;
    }
}
