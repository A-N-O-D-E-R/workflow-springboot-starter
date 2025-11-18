package com.anode.workflow.example.complex.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.complex.model.OrderRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task("arrangeshippingtask")
@AllArgsConstructor
public class ArrangeShippingTask implements InvokableTask {

    private final Object context;

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing ArrangeShippingTask");

        if (context instanceof OrderRequest order) {
            String shippingMethod = order.isInternational() ? "International Courier" : "Domestic Express";
            String trackingNumber = "TRACK-" + System.currentTimeMillis();

            log.info("Arranging {} shipping to: {}", shippingMethod, order.getShippingAddress());
            log.info("Tracking number: {}", trackingNumber);
            log.info("Shipping arrangement completed");
        }

        return new TaskResponse(StepResponseType.OK_PROCEED, null, ".");
    }
}
