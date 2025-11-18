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
@Task("calculateshippingtask")
@AllArgsConstructor
public class CalculateShippingTask implements InvokableTask {

    private final Object context;

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing CalculateShippingTask");

        if (context instanceof OrderRequest order) {
            BigDecimal shippingCost;

            if (order.isInternational()) {
                shippingCost = new BigDecimal("50.00");
                log.info("International shipping cost: {}", shippingCost);
            } else {
                shippingCost = new BigDecimal("10.00");
                log.info("Domestic shipping cost: {}", shippingCost);
            }

            if (order.isRequiresGiftWrap()) {
                shippingCost = shippingCost.add(new BigDecimal("5.00"));
                log.info("Gift wrap fee added");
            }

            log.info("Total shipping cost: {}", shippingCost);
        }

        return new TaskResponse(StepResponseType.OK_PROCEED, null, ".");
    }
}
