package com.anode.workflow.example.concurrent.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task
public class NotifyUserTask implements InvokableTask {

    @Override
    public TaskResponse executeStep() {
        DataProcessingRequest request = (DataProcessingRequest) getWorkflowContext()
            .getVariables()
            .getValue("request");

        String resultId = (String) getWorkflowContext()
            .getVariables()
            .getValue("resultId");

        log.info("[{}] Notifying user {} - Result: {}",
            request.getRequestId(), request.getUserId(), resultId);

        return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
    }
}
