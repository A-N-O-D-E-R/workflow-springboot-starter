# Complex E-Commerce Workflow Example

This example demonstrates a sophisticated e-commerce order processing workflow with **10+ tasks**, **conditional routing**, and **complex business logic**.

## Features Demonstrated

- ✅ **10+ Sequential Tasks** - Complete order processing pipeline
- ✅ **Conditional Routing** - Customer type-based workflow paths
- ✅ **Complex Business Logic** - Discounts, shipping, inventory management
- ✅ **Variable Passing** - Data flow between tasks
- ✅ **Error Handling** - Validation and failure scenarios
- ✅ **Fluent Workflow API** - Clean, readable workflow definitions

## Workflow Architecture

```
ValidateOrder → CustomerTypeRoute → ApplyDiscount → CheckInventory
    → ReserveInventory → CalculateShipping → ProcessPayment
    → UpdateInventory → NotifyWarehouse → ArrangeShipping
    → SendConfirmation
```

### Task Breakdown

| Step | Task | Description |
|------|------|-------------|
| 1 | `ValidateOrderTask` | Validates order data and business rules |
| 2 | `CustomerTypeRoute` | Routes based on customer type (VIP/Corporate/Regular) |
| 3 | `ApplyDiscountTask` | Applies customer-specific discounts |
| 4 | `CheckInventoryTask` | Verifies product availability |
| 5 | `ReserveInventoryTask` | Reserves items for the order |
| 6 | `CalculateShippingTask` | Calculates shipping costs (domestic/international) |
| 7 | `ProcessPaymentTask` | Processes payment transaction |
| 8 | `UpdateInventoryTask` | Updates inventory levels |
| 9 | `NotifyWarehouseTask` | Alerts warehouse for fulfillment |
| 10 | `ArrangeShippingTask` | Creates shipping labels and tracking |
| 11 | `SendConfirmationEmailTask` | Sends order confirmation to customer |

### Conditional Logic

The **CustomerTypeRoute** demonstrates routing based on customer type:

- **VIP Customers** → Priority discount processing (20% off)
- **Corporate Customers** → Bulk discount processing (15% off)
- **Regular/New Customers** → Standard processing (10% promo code discount)

## Running the Example

### Prerequisites

```bash
# From the root project directory
mvn clean install
```

### Start the Application

```bash
cd example/complex-ecommerce-workflow
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Process Sample VIP Order
```bash
curl -X POST http://localhost:8080/api/orders/sample/vip
```

**Response:**
```json
{
  "status": "success",
  "orderId": "ORD-abc12345",
  "customerType": "VIP",
  "message": "VIP order processed with priority"
}
```

### Process Sample Corporate Order
```bash
curl -X POST http://localhost:8080/api/orders/sample/corporate
```

### Process Sample International Order
```bash
curl -X POST http://localhost:8080/api/orders/sample/international
```

### Process Custom Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "customerEmail": "customer@example.com",
    "customerType": "VIP",
    "items": [
      {
        "productId": "PROD-001",
        "productName": "Laptop",
        "quantity": 1,
        "unitPrice": 1200.00,
        "inStock": true,
        "warehouseLocation": "Warehouse-A"
      }
    ],
    "totalAmount": 1200.00,
    "shippingAddress": "123 Main St, City, Country",
    "billingAddress": "123 Main St, City, Country",
    "paymentMethod": "CREDIT_CARD",
    "requiresGiftWrap": false,
    "isInternational": false
  }'
```

## Example Output

When you run a VIP order, you'll see detailed logging:

```
================================================
Starting complex workflow for order: ORD-abc12345
Customer: CUST-xyz789 (VIP)
Items: 3, Total: 1325.00
================================================

==> Executing ValidateOrderTask
Validating order: ORD-abc12345
Customer: CUST-xyz789 (Type: VIP)
Total amount: 1325.00
Items count: 3
Order validation successful

==> Executing CustomerTypeRoute
Routing to VIP processing
Next step: applyDiscountTask

==> Executing ApplyDiscountTask
VIP discount applied: 265.00
Final amount after discount: 1060.00

==> Executing CheckInventoryTask
All items in stock

==> Executing ReserveInventoryTask
Reserving 1 units of Laptop from warehouse Warehouse-A
Reserving 2 units of Mouse from warehouse Warehouse-B
Reserving 1 units of Keyboard from warehouse Warehouse-A

==> Executing CalculateShippingTask
Domestic shipping cost: 10.00

==> Executing ProcessPaymentTask
Processing payment for order: ORD-abc12345
Payment method: CREDIT_CARD
Amount to charge: 1060.00
Payment processed successfully

==> Executing UpdateInventoryTask
Updating inventory: Deducting 1 units of Laptop
Updating inventory: Deducting 2 units of Mouse
Updating inventory: Deducting 1 units of Keyboard

==> Executing NotifyWarehouseTask
Notifying warehouse Warehouse-A to prepare 1 units of Laptop
Notifying warehouse Warehouse-B to prepare 2 units of Mouse
Notifying warehouse Warehouse-A to prepare 1 units of Keyboard

==> Executing ArrangeShippingTask
Arranging Domestic Express shipping to: 123 Main St, City, Country
Tracking number: TRACK-1731712345678

==> Executing SendConfirmationEmailTask
Sending confirmation email to: customer@example.com
Order ID: ORD-abc12345
Final Amount: 1060.00
Tracking Number: TRACK-1731712345678

================================================
Complex workflow completed for order: ORD-abc12345
================================================
```

## Key Implementation Details

### Fluent Workflow Definition

```java
workflowFactory.builder(order.getOrderId())
    .task("validateordertask")
    .task("customertyperoute")
    .task("applydiscounttask")
    .task("checkinventorytask")
    .task("reserveinventorytask")
    .task("calculateshippingtask")
    .task("processpaymenttask")
    .task("updateinventorytask")
    .task("notifywarehousetask")
    .task("arrangeshippingtask")
    .task("sendconfirmationemailtask")
    .variable("order", order)
    .start();
```

### Task Auto-Discovery

Tasks are automatically discovered using the `@Task` annotation:

```java
@Slf4j
@Task
public class ValidateOrderTask implements InvokableTask {
    @Override
    public TaskResponse executeStep() {
        // Task implementation
    }
}
```

### Conditional Routing

```java
@Slf4j
@Task
public class CustomerTypeRoute implements InvokableRoute {
    @Override
    public String executeRoute() {
        OrderRequest order = (OrderRequest) getWorkflowContext()
            .getVariables()
            .getValue("order");

        return switch (order.getCustomerType()) {
            case VIP -> "applyDiscountTask";
            case CORPORATE -> "applyDiscountTask";
            default -> "checkInventoryTask";
        };
    }
}
```

## Learning Points

1. **Task Composition** - Building complex workflows from simple, reusable tasks
2. **Conditional Logic** - Using routes to create dynamic workflow paths
3. **State Management** - Passing data between tasks via workflow variables
4. **Error Handling** - Validation and failure handling at each step
5. **Business Logic** - Real-world e-commerce scenarios (discounts, inventory, shipping)

## Next Steps

- Modify discount percentages in `ApplyDiscountTask`
- Add more customer types and routing logic
- Implement fraud detection task
- Add inventory reservation timeout
- Integrate with real payment gateway

## Related Examples

- `simple-payment-processing` - Basic workflow example
- `concurrent-workflow-execution` - Concurrent processing example
