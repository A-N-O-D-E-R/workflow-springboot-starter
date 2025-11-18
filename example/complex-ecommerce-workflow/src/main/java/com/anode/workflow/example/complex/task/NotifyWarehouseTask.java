package com.anode.workflow.example.complex.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.complex.model.OrderRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task("notifywarehousetask")
@AllArgsConstructor
public class NotifyWarehouseTask implements InvokableTask {

    private final Object context;

    @Override
    public TaskResponse executeStep() {
        log.info("==> Executing NotifyWarehouseTask");

        if (context instanceof OrderRequest order) {
            for (OrderRequest.OrderItem item : order.getItems()) {
                log.info("Notifying warehouse {} to prepare {} units of {}",
                    item.getWarehouseLocation(), item.getQuantity(), item.getProductName());
            }
        }

        return new TaskResponse(StepResponseType.OK_PROCEED, null, ".");
    }
}
