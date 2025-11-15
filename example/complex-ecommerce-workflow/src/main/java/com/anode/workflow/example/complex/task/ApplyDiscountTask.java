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
public class ApplyDiscountTask implements InvokableTask {

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing ApplyDiscountTask");

        OrderRequest order = (OrderRequest) getWorkflowContext().getVariables().getValue("order");

        BigDecimal discount = BigDecimal.ZERO;

        // VIP customers get 20% discount
        if (order.getCustomerType() == OrderRequest.CustomerType.VIP) {
            discount = order.getTotalAmount().multiply(new BigDecimal("0.20"));
            log.info("VIP discount applied: {}", discount);
        }
        // Corporate customers get 15% discount
        else if (order.getCustomerType() == OrderRequest.CustomerType.CORPORATE) {
            discount = order.getTotalAmount().multiply(new BigDecimal("0.15"));
            log.info("Corporate discount applied: {}", discount);
        }
        // Promo code discount
        else if (order.getPromoCode() != null && !order.getPromoCode().isEmpty()) {
            discount = order.getTotalAmount().multiply(new BigDecimal("0.10"));
            log.info("Promo code discount applied: {}", discount);
        }

        BigDecimal finalAmount = order.getTotalAmount().subtract(discount);
        getWorkflowContext().getVariables().setValue("finalAmount", finalAmount);
        getWorkflowContext().getVariables().setValue("discountAmount", discount);

        log.info("Final amount after discount: {}", finalAmount);

        return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
    }
}
