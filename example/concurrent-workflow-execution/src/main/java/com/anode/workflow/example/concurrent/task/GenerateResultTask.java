package com.anode.workflow.example.concurrent.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task("generateresulttask")
@AllArgsConstructor
public class GenerateResultTask implements InvokableTask {

    private final Object context;

    @Override
    public TaskResponse executeStep() {
        if (context instanceof DataProcessingRequest request) {
            log.info("[{}] Generating result",
                request.getRequestId());

            // Simulate result generation
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String resultId = "RESULT-" + request.getRequestId();

            log.info("[{}] Result generated: {}", request.getRequestId(), resultId);

            return new TaskResponse(StepResponseType.OK_PROCEED, null, ".");
        }
        return new TaskResponse(StepResponseType.ERROR_PEND, "Invalid context", ".");
    }
}
