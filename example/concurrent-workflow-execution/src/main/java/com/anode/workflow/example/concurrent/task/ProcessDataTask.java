package com.anode.workflow.example.concurrent.task;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task("processdatatask")
@AllArgsConstructor
public class ProcessDataTask implements InvokableTask {

    private final Object context;

    @Override
    public TaskResponse executeStep() {
        if (context instanceof DataProcessingRequest request) {
            log.info("[{}] Processing {} data items...",
                request.getRequestId(), request.getDataItems().size());

            long startTime = System.currentTimeMillis();

            // Simulate heavy processing
            int processedCount = 0;
            for (String item : request.getDataItems()) {
                simulateHeavyProcessing(request.getType());
                processedCount++;
            }

            long duration = System.currentTimeMillis() - startTime;

            log.info("[{}] Processed {} items in {}ms",
                request.getRequestId(), processedCount, duration);

            return new TaskResponse(StepResponseType.OK_PROCEED, null, ".");
        }
        return new TaskResponse(StepResponseType.ERROR_PEND, "Invalid context", ".");
    }

    private void simulateHeavyProcessing(DataProcessingRequest.ProcessingType type) {
        long processingTime = switch (type) {
            case IMAGE_PROCESSING -> 200;
            case VIDEO_PROCESSING -> 500;
            case DOCUMENT_PROCESSING -> 150;
            case DATA_ANALYSIS -> 300;
            case REPORT_GENERATION -> 400;
        };

        try {
            Thread.sleep(processingTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
