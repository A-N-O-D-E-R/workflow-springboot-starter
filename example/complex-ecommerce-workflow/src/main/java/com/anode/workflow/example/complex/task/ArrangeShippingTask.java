package com.anode.workflow.example.complex.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.complex.model.OrderRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task
public class ArrangeShippingTask implements InvokableTask {

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing ArrangeShippingTask");

        OrderRequest order = (OrderRequest) getWorkflowContext().getVariables().getValue("order");

        String shippingMethod = order.isInternational() ? "International Courier" : "Domestic Express";
        String trackingNumber = "TRACK-" + System.currentTimeMillis();

        log.info("Arranging {} shipping to: {}", shippingMethod, order.getShippingAddress());
        log.info("Tracking number: {}", trackingNumber);

        getWorkflowContext().getVariables().setValue("trackingNumber", trackingNumber);
        getWorkflowContext().getVariables().setValue("shippingMethod", shippingMethod);

        return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
    }
}
