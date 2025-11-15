package com.anode.workflow.example.concurrent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Concurrent Workflow Execution Example Application.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Asynchronous workflow execution with @Async</li>
 *   <li>Multiple workflows running concurrently</li>
 *   <li>Thread-safe workflow state management</li>
 *   <li>Batch processing with controlled concurrency</li>
 *   <li>Real-time workflow statistics</li>
 *   <li>Stress testing capabilities</li>
 * </ul>
 *
 * <p>To run:
 * <pre>
 * mvn spring-boot:run
 * </pre>
 *
 * <p>Test endpoints:
 * <pre>
 * # Process 10 requests concurrently
 * curl -X POST http://localhost:8081/api/processing/batch/small
 *
 * # Process 50 requests concurrently
 * curl -X POST http://localhost:8081/api/processing/batch/medium
 *
 * # Process 100 requests with controlled concurrency
 * curl -X POST http://localhost:8081/api/processing/batch/large
 *
 * # Stress test with custom count
 * curl -X POST "http://localhost:8081/api/processing/stress-test?count=200"
 *
 * # Get statistics
 * curl http://localhost:8081/api/processing/stats
 * </pre>
 */
@SpringBootApplication
@EnableAsync
public class ConcurrentWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcurrentWorkflowApplication.class, args);
    }

    /**
     * Configure thread pool for async workflow execution.
     *
     * <p>Adjust these settings based on your system resources:
     * <ul>
     *   <li>corePoolSize: Minimum threads always active</li>
     *   <li>maxPoolSize: Maximum concurrent workflows</li>
     *   <li>queueCapacity: Pending workflow queue size</li>
     * </ul>
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);      // Min threads
        executor.setMaxPoolSize(50);       // Max concurrent workflows
        executor.setQueueCapacity(100);    // Queue size
        executor.setThreadNamePrefix("workflow-");
        executor.initialize();
        return executor;
    }
}
