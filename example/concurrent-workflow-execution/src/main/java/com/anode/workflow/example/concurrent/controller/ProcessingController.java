package com.anode.workflow.example.concurrent.controller;

import com.anode.workflow.example.concurrent.model.DataProcessingRequest;
import com.anode.workflow.example.concurrent.service.ConcurrentWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller demonstrating concurrent workflow execution.
 */
@Slf4j
@RestController
@RequestMapping("/api/processing")
@RequiredArgsConstructor
public class ProcessingController {

    private final ConcurrentWorkflowService workflowService;

    /**
     * Process a single request asynchronously.
     */
    @PostMapping("/single")
    public ResponseEntity<Map<String, String>> processSingle(@RequestBody DataProcessingRequest request) {
        if (request.getRequestId() == null) {
            request.setRequestId("REQ-" + UUID.randomUUID().toString().substring(0, 8));
        }
        request.setSubmittedAt(System.currentTimeMillis());

        workflowService.processRequestAsync(request);

        return ResponseEntity.ok(Map.of(
            "status", "accepted",
            "requestId", request.getRequestId(),
            "message", "Processing started asynchronously"
        ));
    }

    /**
     * Process multiple requests concurrently (10 requests).
     */
    @PostMapping("/batch/small")
    public ResponseEntity<Map<String, Object>> processSmallBatch() {
        List<DataProcessingRequest> requests = generateRequests(10);

        long startTime = System.currentTimeMillis();
        workflowService.processBatchConcurrently(requests);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
            "status", "completed",
            "requestsProcessed", requests.size(),
            "duration", duration + "ms",
            "message", "Processed 10 requests concurrently"
        ));
    }

    /**
     * Process many requests concurrently (50 requests).
     */
    @PostMapping("/batch/medium")
    public ResponseEntity<Map<String, Object>> processMediumBatch() {
        List<DataProcessingRequest> requests = generateRequests(50);

        long startTime = System.currentTimeMillis();
        workflowService.processBatchConcurrently(requests);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
            "status", "completed",
            "requestsProcessed", requests.size(),
            "duration", duration + "ms",
            "averagePerRequest", (duration / requests.size()) + "ms",
            "message", "Processed 50 requests concurrently"
        ));
    }

    /**
     * Process large batch with controlled concurrency (100 requests, batch size 20).
     */
    @PostMapping("/batch/large")
    public ResponseEntity<Map<String, Object>> processLargeBatch() {
        List<DataProcessingRequest> requests = generateRequests(100);

        long startTime = System.currentTimeMillis();
        workflowService.processWithControlledConcurrency(requests, 20);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
            "status", "completed",
            "requestsProcessed", requests.size(),
            "duration", duration + "ms",
            "averagePerRequest", (duration / requests.size()) + "ms",
            "batchSize", 20,
            "message", "Processed 100 requests with controlled concurrency"
        ));
    }

    /**
     * Get current workflow statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<ConcurrentWorkflowService.WorkflowStats> getStats() {
        return ResponseEntity.ok(workflowService.getStats());
    }

    /**
     * Reset workflow statistics.
     */
    @PostMapping("/stats/reset")
    public ResponseEntity<Map<String, String>> resetStats() {
        workflowService.resetStats();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Statistics reset"
        ));
    }

    /**
     * Stress test - process many workflows concurrently.
     */
    @PostMapping("/stress-test")
    public ResponseEntity<Map<String, Object>> stressTest(@RequestParam(defaultValue = "100") int count) {
        List<DataProcessingRequest> requests = generateRequests(count);

        long startTime = System.currentTimeMillis();
        workflowService.processBatchConcurrently(requests);
        long duration = System.currentTimeMillis() - startTime;

        ConcurrentWorkflowService.WorkflowStats stats = workflowService.getStats();

        return ResponseEntity.ok(Map.of(
            "status", "completed",
            "requestsProcessed", count,
            "duration", duration + "ms",
            "averagePerRequest", (duration / count) + "ms",
            "stats", stats,
            "throughput", String.format("%.2f requests/second", (count * 1000.0 / duration))
        ));
    }

    private List<DataProcessingRequest> generateRequests(int count) {
        List<DataProcessingRequest> requests = new ArrayList<>();
        DataProcessingRequest.ProcessingType[] types = DataProcessingRequest.ProcessingType.values();

        for (int i = 0; i < count; i++) {
            DataProcessingRequest request = new DataProcessingRequest();
            request.setRequestId("REQ-" + UUID.randomUUID().toString().substring(0, 8));
            request.setUserId("USER-" + (i % 10));  // Simulate 10 users
            request.setType(types[i % types.length]);
            request.setDataItems(List.of("item1", "item2", "item3"));
            request.setPriority(i % 3);
            request.setSubmittedAt(System.currentTimeMillis());

            requests.add(request);
        }

        return requests;
    }
}
