package com.anode.workflow.example.concurrent.service;

import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import com.anode.workflow.example.concurrent.strategy.EngineSelectionStrategy;
import com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
    private final Map<String, EngineSelectionStrategy> strategies;

    @Value("${workflow.engine-selection.strategy:ROUND_ROBIN}")
    private String defaultStrategyName;

    @Value("${workflow.engine-selection.available-engines:processing-engine-0,processing-engine-1,processing-engine-2,processing-engine-3}")
    private String[] availableEngines;

    @Value("${workflow.retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Value("${workflow.retry.initial-backoff-ms:100}")
    private long initialBackoffMs;

    @Value("${workflow.retry.max-backoff-ms:5000}")
    private long maxBackoffMs;

    // Thread-safe counters for statistics
    private final AtomicInteger totalWorkflowsStarted = new AtomicInteger(0);
    private final AtomicInteger totalWorkflowsCompleted = new AtomicInteger(0);
    private final AtomicInteger totalWorkflowsFailed = new AtomicInteger(0);
    private final AtomicInteger totalRetries = new AtomicInteger(0);
    private final AtomicInteger totalBusyExceptions = new AtomicInteger(0);

    // Thread-safe map to track workflow status
    private final ConcurrentHashMap<String, WorkflowStatus> workflowStatuses = new ConcurrentHashMap<>();

    /**
     * Execute a single workflow asynchronously using the default strategy.
     *
     * <p>This method returns immediately while the workflow executes in the background.
     */
    @Async
    public CompletableFuture<String> processRequestAsync(DataProcessingRequest request) {
        return processRequestAsync(request, defaultStrategyName);
    }

    /**
     * Execute a single workflow asynchronously using a specific strategy.
     *
     * <p>This method returns immediately while the workflow executes in the background.
     *
     * @param request The data processing request
     * @param strategyName The engine selection strategy to use (ROUND_ROBIN, PRIORITY_BASED, etc.)
     */
    @Async
    public CompletableFuture<String> processRequestAsync(DataProcessingRequest request, String strategyName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info(">>> Starting async workflow for request: {}", request.getRequestId());
                workflowStatuses.put(request.getRequestId(), WorkflowStatus.RUNNING);
                totalWorkflowsStarted.incrementAndGet();

                // Select engine using the specified strategy
                EngineSelectionStrategy strategy = getStrategy(strategyName);
                String engineName = strategy.selectEngine(request, availableEngines);

                log.info("[{}] Using strategy: {} → Engine: {}",
                    request.getRequestId(), strategy.getStrategyName(), engineName);

                // Execute the workflow with retry logic for RuntimeBusyException
                executeWorkflowWithRetry(request, engineName);

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
     * Execute workflow with exponential backoff retry logic for RuntimeBusyException.
     *
     * <p>When a RuntimeBusyException is thrown, it means the RuntimeService is currently
     * processing another case start. This method retries with exponential backoff:
     * <ul>
     *   <li>Initial backoff: 100ms (configurable)</li>
     *   <li>Exponential increase: backoff *= 2</li>
     *   <li>Max backoff: 5000ms (configurable)</li>
     *   <li>Max attempts: 5 (configurable)</li>
     * </ul>
     *
     * @param request The data processing request
     * @param engineName The selected engine name
     * @throws RuntimeBusyException if max retry attempts exceeded
     */
    private void executeWorkflowWithRetry(DataProcessingRequest request, String engineName) {
        int attempt = 0;
        long backoffMs = initialBackoffMs;
        RuntimeException lastException = null;

        while (attempt < maxRetryAttempts) {
            try {
                // Attempt to start the workflow
                workflowFactory.builder(request.getRequestId())
                    .engine(engineName)
                    .task("validaterequesttask")
                    .task("processdatatask")
                    .task("generateresulttask")
                    .task("notifyusertask")
                    .variable("request", request)
                    .variable("startTime", System.currentTimeMillis())
                    .start();

                // Success! Log if we had to retry
                if (attempt > 0) {
                    log.info("[{}] Successfully started workflow after {} retries",
                        request.getRequestId(), attempt);
                }
                return; // Exit on success

            } catch (RuntimeException e) {
                lastException = e;
                attempt++;
                totalBusyExceptions.incrementAndGet();

                if (attempt >= maxRetryAttempts) {
                    // Max retries exceeded
                    log.error("[{}] Failed to start workflow after {} attempts. RuntimeService is busy.",
                        request.getRequestId(), maxRetryAttempts);
                    workflowStatuses.put(request.getRequestId(), WorkflowStatus.QUEUED);
                    throw new RuntimeException(
                        String.format("Failed to start workflow for request %s after %d attempts. " +
                            "RuntimeService is busy processing other cases. Please try again later.",
                            request.getRequestId(), maxRetryAttempts));
                }

                // Log retry attempt
                log.warn("[{}] RuntimeService busy (attempt {}/{}). Retrying in {}ms... Reason: {}",
                    request.getRequestId(), attempt, maxRetryAttempts, backoffMs, e.getMessage());

                totalRetries.incrementAndGet();

                // Wait with exponential backoff
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("[{}] Interrupted during retry backoff", request.getRequestId());
                    throw new RuntimeException("Workflow execution interrupted during retry", ie);
                }

                // Increase backoff exponentially (with max cap)
                backoffMs = Math.min(backoffMs * 2, maxBackoffMs);
            }
        }

        // Should never reach here, but throw last exception as safety
        if (lastException != null) {
            throw lastException;
        }
    }

    /**
     * Get a strategy by name, falling back to default if not found.
     */
    private EngineSelectionStrategy getStrategy(String strategyName) {
        String key = strategyName.toLowerCase().replace("_", "") + "Strategy";
        EngineSelectionStrategy strategy = strategies.get(key);

        if (strategy == null) {
            log.warn("Strategy '{}' not found, using default strategy", strategyName);
            // Fallback to first available strategy
            strategy = strategies.values().iterator().next();
        }

        return strategy;
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
            totalWorkflowsStarted.get() - totalWorkflowsCompleted.get() - totalWorkflowsFailed.get(),
            totalRetries.get(),
            totalBusyExceptions.get()
        );
    }

    /**
     * Get information about available strategies.
     */
    public List<String> getAvailableStrategies() {
        return strategies.values().stream()
            .map(EngineSelectionStrategy::getStrategyName)
            .sorted()
            .toList();
    }

    /**
     * Get the currently configured default strategy.
     */
    public String getDefaultStrategy() {
        return defaultStrategyName;
    }

    /**
     * Get the list of available engines.
     */
    public String[] getAvailableEngines() {
        return availableEngines;
    }

    /**
     * Reset statistics.
     */
    public void resetStats() {
        totalWorkflowsStarted.set(0);
        totalWorkflowsCompleted.set(0);
        totalWorkflowsFailed.set(0);
        totalRetries.set(0);
        totalBusyExceptions.set(0);
        workflowStatuses.clear();
    }

    public enum WorkflowStatus {
        RUNNING, COMPLETED, FAILED, QUEUED
    }

    public record WorkflowStats(
        int totalStarted,
        int totalCompleted,
        int totalFailed,
        int currentlyRunning,
        int totalRetries,
        int totalBusyExceptions
    ) {}
}
