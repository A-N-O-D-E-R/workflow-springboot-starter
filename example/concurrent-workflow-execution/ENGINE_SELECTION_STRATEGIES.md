# Engine Selection Strategies

This document describes the various engine selection strategies available in the concurrent workflow execution example.

## Overview

The concurrent workflow example demonstrates how to distribute workflow executions across multiple workflow engines using different selection strategies. Each strategy has specific use cases and characteristics.

## Available Strategies

### 1. Round Robin (`ROUND_ROBIN`)

**Description:** Distributes requests evenly across all available engines in a circular fashion.

**Use Cases:**
- Balanced load distribution
- No special affinity requirements
- Simple and predictable distribution

**Implementation:**
- Uses thread-safe `AtomicInteger` counter
- Guarantees even distribution over time
- No request-specific routing logic

**Example:**
```
Request 1 → engine-0
Request 2 → engine-1
Request 3 → engine-2
Request 4 → engine-3
Request 5 → engine-0 (cycles back)
```

---

### 2. Priority Based (`PRIORITY_BASED`)

**Description:** Routes requests based on their priority level to dedicated or shared engines.

**Routing Rules:**
- **High Priority (7-10):** → `processing-engine-0` (dedicated for urgent tasks)
- **Medium Priority (4-6):** → `processing-engine-1` or `processing-engine-2` (balanced)
- **Low Priority (0-3):** → `processing-engine-3` (background processing)

**Use Cases:**
- SLA-based processing
- Critical vs. background workloads
- Resource isolation by importance

**Benefits:**
- High-priority tasks get dedicated resources
- Prevents low-priority tasks from impacting critical work
- Clear separation of concerns

---

### 3. Hash Based (`HASH_BASED`)

**Description:** Uses consistent hashing on the request ID to select engines.

**Characteristics:**
- Deterministic: Same request ID always routes to same engine
- Thread-safe and collision-resistant
- Uses Java's `hashCode()` for distribution

**Use Cases:**
- Request affinity requirements
- Stateful workflow processing
- Cache locality optimization
- Debugging and troubleshooting (predictable routing)

**Example:**
```
Request "REQ-abc123" → always routes to engine-2
Request "REQ-xyz789" → always routes to engine-1
```

---

### 4. Type Based (`TYPE_BASED`)

**Description:** Routes requests to specific engines based on their processing type.

**Routing Map:**
- `VIDEO_PROCESSING` → `processing-engine-0` (most resource-intensive)
- `IMAGE_PROCESSING` → `processing-engine-1` (moderate resources)
- `DOCUMENT_PROCESSING` → `processing-engine-2` (I/O intensive)
- `DATA_ANALYSIS` → `processing-engine-3` (CPU intensive)
- `REPORT_GENERATION` → Last available engine (background)

**Use Cases:**
- Workload-specific optimization
- Resource specialization
- Engine tuning per processing type
- Mixed workload scenarios

**Benefits:**
- Engines can be optimized for specific workload types
- Better resource utilization
- Predictable performance per type

---

### 5. Random (`RANDOM`)

**Description:** Randomly distributes requests across all available engines.

**Implementation:**
- Uses `ThreadLocalRandom` for better concurrency performance
- No predictability or affinity
- Simple probabilistic distribution

**Use Cases:**
- Simple load balancing
- Testing and development
- No special routing requirements

**Note:** While random distribution is simple, it may not be as balanced as round-robin over small sample sizes.

---

### 6. User Affinity (`USER_AFFINITY`)

**Description:** Routes all requests from the same user to the same engine.

**Characteristics:**
- Consistent hashing on `userId`
- Same user always uses same engine
- Thread-safe and deterministic

**Use Cases:**
- User session management
- User-specific caching
- State preservation per user
- User-isolated processing

**Benefits:**
- Maintains user context and state
- Better cache hit rates for user data
- Simplified session management
- User-level resource isolation

**Example:**
```
All requests from "USER-5" → always route to engine-2
All requests from "USER-7" → always route to engine-0
```

---

## Configuration

### Application Properties

Edit `application.yml` to configure the strategy:

```yaml
workflow:
  engine-selection:
    # Select the strategy to use
    strategy: ROUND_ROBIN  # Change to: PRIORITY_BASED, HASH_BASED, TYPE_BASED, RANDOM, USER_AFFINITY

    # Define available engines (must match engine configurations)
    available-engines: processing-engine-0,processing-engine-1,processing-engine-2,processing-engine-3
```

### Programmatic Selection

You can also select a strategy programmatically per request:

```java
// Use default strategy from config
workflowService.processRequestAsync(request);

// Specify strategy explicitly
workflowService.processRequestAsync(request, "PRIORITY_BASED");
```

---

## REST API

### Get Available Strategies

```bash
GET /api/processing/strategies
```

**Response:**
```json
{
  "available": ["HASH_BASED", "PRIORITY_BASED", "RANDOM", "ROUND_ROBIN", "TYPE_BASED", "USER_AFFINITY"],
  "default": "ROUND_ROBIN",
  "engines": ["processing-engine-0", "processing-engine-1", "processing-engine-2", "processing-engine-3"]
}
```

### Process with Specific Strategy

```bash
POST /api/processing/single/PRIORITY_BASED
Content-Type: application/json

{
  "userId": "USER-123",
  "type": "VIDEO_PROCESSING",
  "dataItems": ["item1", "item2"],
  "priority": 8
}
```

---

## Strategy Selection Guide

| Requirement | Recommended Strategy | Reason |
|------------|---------------------|---------|
| Even load distribution | `ROUND_ROBIN` | Simplest, most balanced |
| SLA/Priority handling | `PRIORITY_BASED` | Resource isolation by importance |
| Same request → same engine | `HASH_BASED` | Consistent hashing on request ID |
| Workload specialization | `TYPE_BASED` | Optimize engines per workload type |
| User session management | `USER_AFFINITY` | Keep user data on same engine |
| Testing/Development | `RANDOM` | Simple and unpredictable |

---

## Performance Considerations

### Thread Safety
All strategies are thread-safe and designed for concurrent execution:
- `RoundRobinStrategy`: Uses `AtomicInteger`
- `HashBasedStrategy`: Deterministic, no shared state
- `UserAffinityStrategy`: Deterministic, no shared state
- `TypeBasedStrategy`: Stateless
- `PriorityBasedStrategy`: Stateless
- `RandomStrategy`: Uses `ThreadLocalRandom`

### Scalability
- All strategies scale linearly with the number of engines
- Add more engines to increase throughput
- Strategies automatically adapt to available engine count

### Monitoring
Check engine distribution in logs:
```
[REQ-abc123] Using strategy: PRIORITY_BASED → Engine: processing-engine-0
```

---

## Custom Strategies

To implement a custom strategy:

1. Create a class implementing `EngineSelectionStrategy`:
```java
@Component("myCustomStrategy")
public class MyCustomStrategy implements EngineSelectionStrategy {
    @Override
    public String selectEngine(DataProcessingRequest request, String[] availableEngines) {
        // Your selection logic here
        return availableEngines[0];
    }

    @Override
    public String getStrategyName() {
        return "MY_CUSTOM";
    }
}
```

2. Update `application.yml`:
```yaml
workflow:
  engine-selection:
    strategy: MY_CUSTOM
```

---

## Architecture

```
┌─────────────────────────────────────────┐
│   ConcurrentWorkflowService             │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │  EngineSelectionStrategy          │ │
│  │  (Strategy Interface)             │ │
│  └───────────────────────────────────┘ │
│            ▲                            │
│            │                            │
│  ┌─────────┴──────────┬────────────┐   │
│  │                    │            │   │
│  ▼                    ▼            ▼   │
│ Round Robin    Priority Based  Hash   │
│                                        │
│  ▼                    ▼            ▼   │
│ Type Based      Random      User Aff. │
└─────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────┐
│  4 Workflow Engines                     │
│  - processing-engine-0                  │
│  - processing-engine-1                  │
│  - processing-engine-2                  │
│  - processing-engine-3                  │
└─────────────────────────────────────────┘
```

---

## Testing Different Strategies

### Example: Test Priority-Based Strategy

```bash
# High priority request → should route to engine-0
curl -X POST http://localhost:8081/api/processing/single/PRIORITY_BASED \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER-1",
    "type": "VIDEO_PROCESSING",
    "dataItems": ["item1"],
    "priority": 9
  }'
```

### Example: Test User Affinity

```bash
# All requests from USER-5 should go to the same engine
for i in {1..5}; do
  curl -X POST http://localhost:8081/api/processing/single/USER_AFFINITY \
    -H "Content-Type: application/json" \
    -d "{\"userId\": \"USER-5\", \"type\": \"IMAGE_PROCESSING\", \"dataItems\": [\"item1\"], \"priority\": 5}"
done
```

Check the logs to verify all 5 requests route to the same engine.

---

## Troubleshooting

### Strategy Not Found
If you see: `Strategy 'XXX' not found, using default strategy`
- Check the strategy name spelling in `application.yml`
- Valid names: `ROUND_ROBIN`, `PRIORITY_BASED`, `HASH_BASED`, `TYPE_BASED`, `RANDOM`, `USER_AFFINITY`

### Uneven Distribution
- `RANDOM` may not distribute evenly over small samples
- Use `ROUND_ROBIN` for guaranteed even distribution

### All Requests Going to Same Engine
- Check if using `HASH_BASED` or `USER_AFFINITY` with same ID
- Verify multiple engines are configured and available

---

**Last Updated:** 2025-11-18
**Version:** 1.0.0
