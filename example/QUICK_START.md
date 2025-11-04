# Quick Start Guide

Get the example application running in 5 minutes!

## Prerequisites

- Java 17+
- Maven 3.6+

## Steps

### 1. Build the Workflow Starter

```bash
cd /home/arthur/Documents/Code/perso/workflow-springboot-starter
mvn clean install
```

### 2. Build and Run the Example

```bash
cd example
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 3. Submit a Test Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @sample-requests/create-order-standard.json
```

**Expected Response:**
```json
{
  "orderId": "ORD-2025-001",
  "status": "COMPLETED",
  "workflowCompleted": true,
  "message": "Order submitted successfully"
}
```

### 4. Check Order Status

```bash
curl http://localhost:8080/api/orders/ORD-2025-001
```

### 5. Run the Test Suite

```bash
mvn test
```

## What's Happening?

When you submit an order, the workflow engine executes these steps automatically:

1. ✅ **Validates** the order data
2. ✅ **Processes** the payment
3. ✅ **Checks** inventory availability
4. ✅ **Routes** based on shipping method (standard/express/overnight)
5. ✅ **Prepares** the shipment
6. ✅ **Sends** notifications to customer

All while tracking state, logging events, and monitoring SLAs!

## Try Different Shipping Methods

```bash
# Standard shipping (5 days)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @sample-requests/create-order-standard.json

# Express shipping (2 days)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @sample-requests/create-order-express.json

# Overnight shipping (1 day)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @sample-requests/create-order-overnight.json
```

## Check the Logs

Watch the console for detailed workflow execution logs:

```
2025-01-04 12:00:00 [main] INFO  c.a.w.e.t.ValidateOrderTask - Validating order for case: ORD-2025-001
2025-01-04 12:00:00 [main] INFO  c.a.w.e.t.ProcessPaymentTask - Processing payment for case: ORD-2025-001
2025-01-04 12:00:00 [main] INFO  c.a.w.e.s.i.MockPaymentService - Payment processed successfully: TransactionId=TXN-ABC123
...
[AUDIT] 2025-01-04 12:00:00 - Case: ORD-2025-001 - Event: WORKFLOW_COMPLETED
```

## Next Steps

- Read the [full README](README.md) for detailed documentation
- Explore the test suite to see TDD in action
- Try different configuration profiles (dev, test, prod)
- Extend with your own tasks and workflows!

## Troubleshooting

**Application won't start?**
- Make sure you built the starter first: `cd .. && mvn clean install`
- Check Java version: `java -version` (should be 17+)

**Tests failing?**
- Ensure clean build: `mvn clean test`
- Check that port 8080 is available

**Can't submit orders?**
- Verify application is running: `curl http://localhost:8080/actuator/health` (if actuator is enabled)
- Check request JSON format matches examples

## Support

For more details, see the comprehensive [README](README.md) or check the workflow starter [documentation](../README.md).
