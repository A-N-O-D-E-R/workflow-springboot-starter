package com.anode.workflow.example.complex.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.complex.model.OrderRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task("checkinventorytask")
@AllArgsConstructor
public class CheckInventoryTask implements InvokableTask {

    private final Object context;

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing CheckInventoryTask");

        if (context instanceof OrderRequest order) {
            boolean allInStock = order.getItems().stream()
                .allMatch(OrderRequest.OrderItem::isInStock);

            if (!allInStock) {
                log.warn("Some items out of stock");
            } else {
                log.info("All items in stock");
            }
        }

        return new TaskResponse(StepResponseType.OK_PROCEED, null, ".");
    }
}
