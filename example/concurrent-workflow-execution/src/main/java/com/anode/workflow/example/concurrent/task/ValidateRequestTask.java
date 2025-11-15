package com.anode.workflow.example.concurrent.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task
public class ValidateRequestTask implements InvokableTask {

    @Override
    public TaskResponse executeStep() {
        DataProcessingRequest request = (DataProcessingRequest) getWorkflowContext()
            .getVariables()
            .getValue("request");

        log.info("[{}] Validating request - Type: {}, Items: {}",
            request.getRequestId(), request.getType(), request.getDataItems().size());

        // Simulate validation time
        simulateProcessing(100);

        if (request.getDataItems().isEmpty()) {
            return new TaskResponse(StepResponseType.FAILED, "noData", null);
        }

        return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
    }

    private void simulateProcessing(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
