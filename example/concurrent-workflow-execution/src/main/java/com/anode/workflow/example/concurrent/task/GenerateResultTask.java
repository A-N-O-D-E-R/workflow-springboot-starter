package com.anode.workflow.example.concurrent.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task
public class GenerateResultTask implements InvokableTask {

    @Override
    public TaskResponse executeStep() {
        DataProcessingRequest request = (DataProcessingRequest) getWorkflowContext()
            .getVariables()
            .getValue("request");

        Integer processedCount = (Integer) getWorkflowContext()
            .getVariables()
            .getValue("processedCount");

        Long duration = (Long) getWorkflowContext()
            .getVariables()
            .getValue("processingDuration");

        log.info("[{}] Generating result - Processed: {}, Duration: {}ms",
            request.getRequestId(), processedCount, duration);

        // Simulate result generation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String resultId = "RESULT-" + request.getRequestId();
        getWorkflowContext().getVariables().setValue("resultId", resultId);

        log.info("[{}] Result generated: {}", request.getRequestId(), resultId);

        return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
    }
}
