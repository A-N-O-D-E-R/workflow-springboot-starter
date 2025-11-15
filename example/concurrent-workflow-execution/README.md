# Concurrent Workflow Execution Example

This example demonstrates **concurrent workflow execution** with **thread-safe state management**, **async processing**, and **real-time statistics**.

## Features Demonstrated

- ✅ **Async Workflow Execution** - Background workflow processing with @Async
- ✅ **Concurrent Processing** - Multiple workflows running simultaneously
- ✅ **Thread-Safe State Management** - Atomic counters and concurrent collections
- ✅ **Batch Processing** - Process multiple requests efficiently
- ✅ **Controlled Concurrency** - Manage system load with batch sizes
- ✅ **Real-Time Statistics** - Track workflow execution metrics
- ✅ **Stress Testing** - Test system limits and throughput

## Architecture

```
Controller → @Async Service → Thread Pool (10-50 threads)
                    ↓
          Concurrent Workflows (thread-safe)
                    ↓
          Memory Storage (synchronized)
                    ↓
          Statistics Tracking (atomic)
```

### Concurrency Model

- **Thread Pool**: 10 core threads, up to 50 max threads
- **Queue**: 100 pending workflows
- **Thread Safety**: Memory storage with synchronized methods
- **Async Execution**: Spring's `@Async` with `CompletableFuture`

## Running the Example

### Prerequisites

```bash
# From the root project directory
mvn clean install
```

### Start the Application

```bash
cd example/concurrent-workflow-execution
mvn spring-boot:run
```

The application will start on `http://localhost:8081`

## API Endpoints

### 1. Process 10 Requests Concurrently

```bash
curl -X POST http://localhost:8081/api/processing/batch/small
```

**Response:**
```json
{
  "status": "completed",
  "requestsProcessed": 10,
  "duration": "2500ms",
  "message": "Processed 10 requests concurrently"
}
```

### 2. Process 50 Requests Concurrently

```bash
curl -X POST http://localhost:8081/api/processing/batch/medium
```

**Response:**
```json
{
  "status": "completed",
  "requestsProcessed": 50,
  "duration": "8500ms",
  "averagePerRequest": "170ms",
  "message": "Processed 50 requests concurrently"
}
```

### 3. Process 100 Requests with Controlled Concurrency

```bash
curl -X POST http://localhost:8081/api/processing/batch/large
```

**Response:**
```json
{
  "status": "completed",
  "requestsProcessed": 100,
  "duration": "15000ms",
  "averagePerRequest": "150ms",
  "batchSize": 20,
  "message": "Processed 100 requests with controlled concurrency"
}
```

### 4. Stress Test (Custom Count)

```bash
curl -X POST "http://localhost:8081/api/processing/stress-test?count=200"
```

**Response:**
```json
{
  "status": "completed",
  "requestsProcessed": 200,
  "duration": "25000ms",
  "averagePerRequest": "125ms",
  "stats": {
    "totalStarted": 200,
    "totalCompleted": 200,
    "totalFailed": 0,
    "currentlyRunning": 0
  },
  "throughput": "8.00 requests/second"
}
```

### 5. Get Real-Time Statistics

```bash
curl http://localhost:8081/api/processing/stats
```

**Response:**
```json
{
  "totalStarted": 360,
  "totalCompleted": 360,
  "totalFailed": 0,
  "currentlyRunning": 0
}
```

### 6. Reset Statistics

```bash
curl -X POST http://localhost:8081/api/processing/stats/reset
```

## Example Output

When processing requests concurrently, you'll see interleaved logging:

```
00:12:34.123 [workflow-1] - [REQ-abc123] Validating request - Type: IMAGE_PROCESSING, Items: 3
00:12:34.125 [workflow-2] - [REQ-def456] Validating request - Type: VIDEO_PROCESSING, Items: 3
00:12:34.127 [workflow-3] - [REQ-ghi789] Validating request - Type: DATA_ANALYSIS, Items: 3

00:12:34.234 [workflow-1] - [REQ-abc123] Processing 3 data items...
00:12:34.236 [workflow-2] - [REQ-def456] Processing 3 data items...
00:12:34.238 [workflow-3] - [REQ-ghi789] Processing 3 data items...

00:12:35.123 [workflow-1] - [REQ-abc123] Processed 3 items in 889ms
00:12:35.456 [workflow-2] - [REQ-def456] Processed 3 items in 1220ms
00:12:35.234 [workflow-3] - [REQ-ghi789] Processed 3 items in 996ms

================================================
Batch processing completed
Total requests: 10
Duration: 2500ms
Average: 250ms per request
================================================
```

## Performance Characteristics

### Processing Times by Type

| Processing Type | Time per Item | Total for 3 Items |
|----------------|---------------|-------------------|
| IMAGE_PROCESSING | 200ms | ~600ms |
| VIDEO_PROCESSING | 500ms | ~1500ms |
| DOCUMENT_PROCESSING | 150ms | ~450ms |
| DATA_ANALYSIS | 300ms | ~900ms |
| REPORT_GENERATION | 400ms | ~1200ms |

### Throughput Benchmarks

| Batch Size | Sequential Time | Concurrent Time | Speedup |
|-----------|-----------------|-----------------|---------|
| 10 requests | ~9000ms | ~2500ms | 3.6x |
| 50 requests | ~45000ms | ~8500ms | 5.3x |
| 100 requests | ~90000ms | ~15000ms | 6.0x |

*Actual times vary based on system resources and request types*

## Key Implementation Details

### Async Workflow Execution

```java
@Async
public CompletableFuture<String> processRequestAsync(DataProcessingRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        workflowFactory.builder(request.getRequestId())
            .task("validaterequesttask")
            .task("processdatatask")
            .task("generateresulttask")
            .task("notifyusertask")
            .variable("request", request)
            .start();

        return request.getRequestId();
    });
}
```

### Thread Pool Configuration

```java
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
```

### Thread-Safe Statistics

```java
private final AtomicInteger totalWorkflowsStarted = new AtomicInteger(0);
private final AtomicInteger totalWorkflowsCompleted = new AtomicInteger(0);
private final ConcurrentHashMap<String, WorkflowStatus> workflowStatuses = new ConcurrentHashMap<>();

// Thread-safe increment
totalWorkflowsStarted.incrementAndGet();
```

### Batch Processing with Controlled Concurrency

```java
public void processWithControlledConcurrency(
    List<DataProcessingRequest> requests,
    int batchSize
) {
    for (int i = 0; i < requests.size(); i += batchSize) {
        int end = Math.min(i + batchSize, requests.size());
        List<DataProcessingRequest> batch = requests.subList(i, end);

        processBatchConcurrently(batch);
    }
}
```

## Thread Safety Guarantees

### Memory Storage (Thread-Safe)

```yaml
workflow:
  engines:
    - name: processing-engine
      storage:
        type: MEMORY  # Synchronized methods for thread safety
```

The Memory storage implementation uses:
- `synchronized` methods for all operations
- `ConcurrentHashMap` for lock tracking
- Thread-safe counter management

### Workflow Isolation

Each workflow execution is isolated:
- ✅ Separate workflow context per request
- ✅ Independent variable scopes
- ✅ No shared mutable state between workflows
- ✅ Thread-safe storage backend

## Stress Testing

### Test System Limits

```bash
# Process 500 requests
curl -X POST "http://localhost:8081/api/processing/stress-test?count=500"

# Process 1000 requests
curl -X POST "http://localhost:8081/api/processing/stress-test?count=1000"
```

### Monitor System Resources

```bash
# Watch thread count
watch -n 1 'jps -v | grep ConcurrentWorkflowApplication'

# Monitor statistics in real-time
watch -n 1 'curl -s http://localhost:8081/api/processing/stats'
```

## Tuning Concurrency

### Adjust Thread Pool Size

Edit `ConcurrentWorkflowApplication.java`:

```java
executor.setCorePoolSize(20);      // Increase for more parallelism
executor.setMaxPoolSize(100);      // Higher max for burst capacity
executor.setQueueCapacity(200);    // Larger queue for buffering
```

### Adjust Batch Sizes

```bash
# Smaller batches for lower memory usage
curl -X POST "http://localhost:8081/api/processing/batch/large"  # Uses batch size 20

# Custom implementation for different batch sizes
```

## Learning Points

1. **Async Processing** - Using Spring's `@Async` for background execution
2. **Thread Safety** - Atomic operations and concurrent collections
3. **Performance** - Concurrent execution dramatically improves throughput
4. **Resource Management** - Thread pool configuration and limits
5. **Monitoring** - Real-time statistics and performance tracking
6. **Scalability** - Handling hundreds of concurrent workflows

## Troubleshooting

### Issue: Workflows Running Sequentially

**Cause**: Thread pool exhausted
**Solution**: Increase `maxPoolSize` or reduce concurrent load

### Issue: Out of Memory

**Cause**: Too many workflows in queue
**Solution**: Reduce `queueCapacity` or use controlled concurrency

### Issue: Poor Performance

**Cause**: Too few threads
**Solution**: Increase `corePoolSize` based on CPU cores

## Best Practices

1. ✅ **Use Thread Pools** - Don't create unlimited threads
2. ✅ **Monitor Statistics** - Track workflow execution metrics
3. ✅ **Handle Failures** - Implement proper error handling
4. ✅ **Control Concurrency** - Use batch processing for large workloads
5. ✅ **Test Limits** - Stress test before production deployment

## Next Steps

- Add retry logic for failed workflows
- Implement priority queues for high-priority requests
- Add distributed tracing for workflow monitoring
- Implement workflow cancellation
- Add metrics integration (Micrometer/Prometheus)

## Related Examples

- `complex-ecommerce-workflow` - Complex task and routing example
- `simple-payment-processing` - Basic workflow example
