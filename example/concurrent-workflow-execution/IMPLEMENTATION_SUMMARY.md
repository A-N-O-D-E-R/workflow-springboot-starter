# RuntimeBusyException Handling - Implementation Summary

## What Was Implemented

The concurrent workflow execution example has been completed with comprehensive `RuntimeBusyException` handling, including retry logic with exponential backoff, metrics tracking, and detailed logging.

## Changes Made

### 1. ConcurrentWorkflowService.java

**File**: `src/main/java/com/anode/workflow/example/concurrent/service/ConcurrentWorkflowService.java`

#### Added Import
```java
import com.anode.workflow.exceptions.RuntimeBusyException;
```

#### Added Configuration Properties
```java
@Value("${workflow.retry.max-attempts:5}")
private int maxRetryAttempts;

@Value("${workflow.retry.initial-backoff-ms:100}")
private long initialBackoffMs;

@Value("${workflow.retry.max-backoff-ms:5000}")
private long maxBackoffMs;
```

#### Added Metrics Counters
```java
private final AtomicInteger totalRetries = new AtomicInteger(0);
private final AtomicInteger totalBusyExceptions = new AtomicInteger(0);
```

#### Implemented Retry Method (lines 118-199)
```java
private void executeWorkflowWithRetry(DataProcessingRequest request, String engineName) {
    // Implements exponential backoff retry logic
    // - Initial backoff: 100ms (configurable)
    // - Exponential increase: backoff *= 2
    // - Max backoff: 5000ms (configurable)
    // - Max attempts: 5 (configurable)
    // - Comprehensive logging at each stage
    // - Metrics tracking for retries and busy exceptions
}
```

**Key Features**:
- Exponential backoff: 100ms → 200ms → 400ms → 800ms → 1600ms
- Max backoff cap at 5000ms to prevent infinite growth
- Detailed logging for each retry attempt
- Marks workflows as QUEUED if max retries exceeded
- Thread interruption handling

#### Updated processRequestAsync() (line 101)
Replaced empty catch block with call to `executeWorkflowWithRetry()`:

**Before**:
```java
try {
    workflowFactory.builder(request.getRequestId())
        .engine(engineName)
        .task("validaterequesttask")
        .task("processdatatask")
        .task("generateresulttask")
        .task("notifyusertask")
        .variable("request", request)
        .variable("startTime", System.currentTimeMillis())
        .start();
} catch(RuntimeBusyException busy) {
    // EMPTY - NEEDS IMPLEMENTATION
}
```

**After**:
```java
// Execute the workflow with retry logic for RuntimeBusyException
executeWorkflowWithRetry(request, engineName);
```

#### Updated WorkflowStatus Enum (line 314)
Added `QUEUED` status for workflows that exceed max retries:

```java
public enum WorkflowStatus {
    RUNNING, COMPLETED, FAILED, QUEUED  // Added QUEUED
}
```

#### Updated WorkflowStats Record (lines 317-324)
Added retry metrics:

```java
public record WorkflowStats(
    int totalStarted,
    int totalCompleted,
    int totalFailed,
    int currentlyRunning,
    int totalRetries,           // NEW
    int totalBusyExceptions     // NEW
) {}
```

#### Updated getStats() Method (lines 270-279)
Returns new metrics:

```java
return new WorkflowStats(
    totalWorkflowsStarted.get(),
    totalWorkflowsCompleted.get(),
    totalWorkflowsFailed.get(),
    totalWorkflowsStarted.get() - totalWorkflowsCompleted.get() - totalWorkflowsFailed.get(),
    totalRetries.get(),         // NEW
    totalBusyExceptions.get()   // NEW
);
```

#### Updated resetStats() Method (lines 308-315)
Resets new counters:

```java
totalRetries.set(0);          // NEW
totalBusyExceptions.set(0);   // NEW
```

### 2. application.yml

**File**: `src/main/resources/application.yml`

#### Added Retry Configuration Section (lines 24-32)

```yaml
workflow:
  # Retry Configuration for RuntimeBusyException
  # When RuntimeService is busy starting another case, the service will retry with exponential backoff
  retry:
    # Maximum number of retry attempts before giving up
    max-attempts: 5
    # Initial backoff delay in milliseconds
    initial-backoff-ms: 100
    # Maximum backoff delay in milliseconds (backoff doubles each retry)
    max-backoff-ms: 5000
```

### 3. Documentation

#### Created RUNTIME_BUSY_HANDLING.md
Comprehensive guide covering:
- When `RuntimeBusyException` is thrown
- Thread-safety model explanation
- Retry mechanism details
- Configuration options
- Metrics and monitoring
- Testing procedures
- Best practices and troubleshooting

#### Created test-runtime-busy.sh
Automated test script that:
- Tests single request (no contention)
- Tests small batch (low contention)
- Tests medium batch (high contention)
- Displays comprehensive statistics
- Validates retry mechanism is working

## How It Works

### Execution Flow

```
1. User calls processRequestAsync(request, strategy)
   ↓
2. Service selects engine using strategy
   ↓
3. Service calls executeWorkflowWithRetry(request, engineName)
   ↓
4. Attempt to start workflow via builder.start()
   ↓
   ┌─ SUCCESS → Return
   │
   └─ RuntimeBusyException thrown
      ↓
      Increment totalBusyExceptions counter
      ↓
      Check: attempt < maxRetryAttempts?
      ├─ YES → Log warning
      │        Increment totalRetries counter
      │        Sleep for backoffMs
      │        Double backoff (capped at maxBackoffMs)
      │        Retry from step 4
      │
      └─ NO  → Mark workflow as QUEUED
               Log error
               Throw RuntimeBusyException
```

### Retry Timeline Example

For a workflow that encounters busy exceptions:

```
0ms:     Attempt 1 → RuntimeBusyException
         ↓ wait 100ms
100ms:   Attempt 2 → RuntimeBusyException
         ↓ wait 200ms
300ms:   Attempt 3 → RuntimeBusyException
         ↓ wait 400ms
700ms:   Attempt 4 → RuntimeBusyException
         ↓ wait 800ms
1500ms:  Attempt 5 → SUCCESS!
```

## Configuration Options

All retry behavior is configurable via `application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `workflow.retry.max-attempts` | 5 | Max retry attempts before giving up |
| `workflow.retry.initial-backoff-ms` | 100 | Starting backoff delay |
| `workflow.retry.max-backoff-ms` | 5000 | Maximum backoff delay cap |

### Tuning Recommendations

**Low contention environment** (few concurrent workflows):
```yaml
workflow:
  retry:
    max-attempts: 3
    initial-backoff-ms: 50
    max-backoff-ms: 2000
```

**High contention environment** (many concurrent workflows):
```yaml
workflow:
  retry:
    max-attempts: 10
    initial-backoff-ms: 100
    max-backoff-ms: 10000
```

**Production environment** (balanced):
```yaml
workflow:
  retry:
    max-attempts: 5
    initial-backoff-ms: 100
    max-backoff-ms: 5000
```

## Metrics Available

The service now tracks:

1. **totalBusyExceptions**: How many times `RuntimeBusyException` was thrown
2. **totalRetries**: Total number of retry attempts made
3. **totalStarted**: Total workflows started
4. **totalCompleted**: Successfully completed workflows
5. **totalFailed**: Failed workflows
6. **currentlyRunning**: Workflows currently executing

**Access via REST API**:
```bash
curl http://localhost:8081/api/processing/stats
```

**Example Response**:
```json
{
  "totalStarted": 60,
  "totalCompleted": 58,
  "totalFailed": 0,
  "currentlyRunning": 2,
  "totalRetries": 23,
  "totalBusyExceptions": 23
}
```

## Testing

### Run the Application
```bash
cd example/concurrent-workflow-execution
mvn spring-boot:run
```

### Run the Test Script
```bash
./test-runtime-busy.sh
```

### Manual Tests

**Single request** (no contention):
```bash
curl -X POST http://localhost:8081/api/processing/single \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-123", "type": "VIDEO", "dataItems": ["video1.mp4"], "priority": 1}'
```

**Small batch** (10 workflows):
```bash
curl -X POST http://localhost:8081/api/processing/batch/small
```

**Medium batch** (50 workflows):
```bash
curl -X POST http://localhost:8081/api/processing/batch/medium
```

**Stress test** (100 workflows):
```bash
curl -X POST "http://localhost:8081/api/processing/stress-test?count=100"
```

**Check statistics**:
```bash
curl http://localhost:8081/api/processing/stats
```

## Expected Results

### Small Batch (10 workflows)
- Total started: 10
- Total completed: 10
- Busy exceptions: 2-8
- Retries: 3-15
- Success rate: 100%

### Medium Batch (50 workflows)
- Total started: 50
- Total completed: 50
- Busy exceptions: 15-35
- Retries: 20-70
- Success rate: ~98%

### Stress Test (100 workflows)
- Total started: 100
- Total completed: 95-100
- Busy exceptions: 30-80
- Retries: 50-150
- Success rate: ~95%+

## Logging Examples

### Successful Retry
```
WARN [REQ-a1b2c3d4] RuntimeService busy (attempt 1/5). Retrying in 100ms...
     Reason: Cannot start case REQ-a1b2c3d4. Another case is currently being started.
WARN [REQ-a1b2c3d4] RuntimeService busy (attempt 2/5). Retrying in 200ms...
     Reason: Cannot start case REQ-a1b2c3d4. Another case is currently being started.
INFO [REQ-a1b2c3d4] Successfully started workflow after 2 retries
```

### Max Retries Exceeded
```
WARN [REQ-x9y8z7w6] RuntimeService busy (attempt 1/5). Retrying in 100ms...
WARN [REQ-x9y8z7w6] RuntimeService busy (attempt 2/5). Retrying in 200ms...
WARN [REQ-x9y8z7w6] RuntimeService busy (attempt 3/5). Retrying in 400ms...
WARN [REQ-x9y8z7w6] RuntimeService busy (attempt 4/5). Retrying in 800ms...
WARN [REQ-x9y8z7w6] RuntimeService busy (attempt 5/5). Retrying in 1600ms...
ERROR [REQ-x9y8z7w6] Failed to start workflow after 5 attempts. RuntimeService is busy.
```

## Files Modified

1. ✅ `ConcurrentWorkflowService.java` - Added retry logic, metrics, and exception handling
2. ✅ `application.yml` - Added retry configuration
3. ✅ `RUNTIME_BUSY_HANDLING.md` - Comprehensive documentation
4. ✅ `IMPLEMENTATION_SUMMARY.md` - This file
5. ✅ `test-runtime-busy.sh` - Automated test script

## Summary

The concurrent workflow execution example now includes:

✅ **Robust exception handling** - Catches and retries `RuntimeBusyException`
✅ **Exponential backoff** - Intelligent retry strategy that adapts to load
✅ **Configurable parameters** - Tune retry behavior via `application.yml`
✅ **Comprehensive metrics** - Track retries, busy exceptions, and success rates
✅ **Detailed logging** - Visibility into every retry attempt
✅ **Production ready** - Handles high concurrency gracefully
✅ **Fully documented** - Complete guide and examples
✅ **Automated testing** - Ready-to-run test script

The implementation demonstrates best practices for handling transient failures in concurrent systems and serves as a reference for building resilient workflow-based applications.
