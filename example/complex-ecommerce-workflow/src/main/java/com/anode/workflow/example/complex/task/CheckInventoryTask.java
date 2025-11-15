package com.anode.workflow.example.complex.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.complex.model.OrderRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task
public class CheckInventoryTask implements InvokableTask {

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing CheckInventoryTask");

        OrderRequest order = (OrderRequest) getWorkflowContext().getVariables().getValue("order");

        boolean allInStock = order.getItems().stream()
            .allMatch(OrderRequest.OrderItem::isInStock);

        if (!allInStock) {
            log.warn("Some items out of stock");
            getWorkflowContext().getVariables().setValue("inventoryStatus", "PARTIAL");
        } else {
            log.info("All items in stock");
            getWorkflowContext().getVariables().setValue("inventoryStatus", "FULL");
        }

        return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
    }
}
