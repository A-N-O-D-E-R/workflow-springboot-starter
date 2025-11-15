package com.anode.workflow.example.concurrent.service;

import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service demonstrating concurrent workflow execution.
 *
 * <p>This service shows how multiple workflows can execute simultaneously
 * in a thread-safe manner using the workflow engine.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Async workflow execution with @Async</li>
 *   <li>Multiple workflows running concurrently</li>
 *   <li>Thread-safe workflow state management</li>
 *   <li>Real-time progress tracking</li>
 *   <li>Concurrent workflow statistics</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConcurrentWorkflowService {

    private final FluentWorkflowBuilderFactory workflowFactory;

    // Thread-safe counters for statistics
    private final AtomicInteger totalWorkflowsStarted = new AtomicInteger(0);
    private final AtomicInteger totalWorkflowsCompleted = new AtomicInteger(0);
    private final AtomicInteger totalWorkflowsFailed = new AtomicInteger(0);

    // Thread-safe map to track workflow status
    private final ConcurrentHashMap<String, WorkflowStatus> workflowStatuses = new ConcurrentHashMap<>();

    /**
     * Execute a single workflow asynchronously.
     *
     * <p>This method returns immediately while the workflow executes in the background.
     */
    @Async
    public CompletableFuture<String> processRequestAsync(DataProcessingRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info(">>> Starting async workflow for request: {}", request.getRequestId());
                workflowStatuses.put(request.getRequestId(), WorkflowStatus.RUNNING);
                totalWorkflowsStarted.incrementAndGet();

                // Execute the workflow
                workflowFactory.builder(request.getRequestId())
                    .task("validaterequesttask")
                    .task("processdatatask")
                    .task("generateresulttask")
                    .task("notifyusertask")
                    .variable("request", request)
                    .variable("startTime", System.currentTimeMillis())
                    .start();

                workflowStatuses.put(request.getRequestId(), WorkflowStatus.COMPLETED);
                totalWorkflowsCompleted.incrementAndGet();

                log.info("<<< Completed async workflow for request: {}", request.getRequestId());
                return request.getRequestId();

            } catch (Exception e) {
                log.error("!!! Failed workflow for request: {}", request.getRequestId(), e);
                workflowStatuses.put(request.getRequestId(), WorkflowStatus.FAILED);
                totalWorkflowsFailed.incrementAndGet();
                throw new RuntimeException("Workflow failed", e);
            }
        });
    }

    /**
     * Execute multiple workflows concurrently.
     *
     * <p>This demonstrates the workflow engine's ability to handle
     * multiple concurrent workflow executions safely.
     */
    public void processBatchConcurrently(List<DataProcessingRequest> requests) {
        log.info("================================================");
        log.info("Starting batch processing of {} requests concurrently", requests.size());
        log.info("================================================");

        long batchStartTime = System.currentTimeMillis();

        // Launch all workflows concurrently
        List<CompletableFuture<String>> futures = requests.stream()
            .map(this::processRequestAsync)
            .toList();

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                long batchDuration = System.currentTimeMillis() - batchStartTime;

                log.info("================================================");
                log.info("Batch processing completed");
                log.info("Total requests: {}", requests.size());
                log.info("Duration: {}ms", batchDuration);
                log.info("Average: {}ms per request", batchDuration / requests.size());
                log.info("================================================");
            })
            .join();
    }

    /**
     * Execute workflows with controlled concurrency.
     *
     * <p>Processes requests in parallel batches to control system load.
     */
    public void processWithControlledConcurrency(List<DataProcessingRequest> requests, int batchSize) {
        log.info("Processing {} requests with batch size {}", requests.size(), batchSize);

        for (int i = 0; i < requests.size(); i += batchSize) {
            int end = Math.min(i + batchSize, requests.size());
            List<DataProcessingRequest> batch = requests.subList(i, end);

            log.info("Processing batch {} - {} (size: {})", i, end, batch.size());
            processBatchConcurrently(batch);
        }
    }

    /**
     * Get current workflow statistics.
     */
    public WorkflowStats getStats() {
        return new WorkflowStats(
            totalWorkflowsStarted.get(),
            totalWorkflowsCompleted.get(),
            totalWorkflowsFailed.get(),
            totalWorkflowsStarted.get() - totalWorkflowsCompleted.get() - totalWorkflowsFailed.get()
        );
    }

    /**
     * Reset statistics.
     */
    public void resetStats() {
        totalWorkflowsStarted.set(0);
        totalWorkflowsCompleted.set(0);
        totalWorkflowsFailed.set(0);
        workflowStatuses.clear();
    }

    public enum WorkflowStatus {
        RUNNING, COMPLETED, FAILED
    }

    public record WorkflowStats(
        int totalStarted,
        int totalCompleted,
        int totalFailed,
        int currentlyRunning
    ) {}
}
