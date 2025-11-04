package com.anode.workflow.example.tasks;

import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowVariable;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.example.model.*;
import com.anode.workflow.example.service.InventoryService;
import com.anode.workflow.example.service.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInventoryTaskTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderRepository orderRepository;

    private CheckInventoryTask checkInventoryTask;

    private WorkflowContext context;
    private Task task;

    @BeforeEach
    void setProcessVariablesUp() {
        checkInventoryTask = new CheckInventoryTask(inventoryService, orderRepository);

        context = new WorkflowContext();
        context.setProcessVariablesCaseId("ORDER-001");
        context.setProcessVariablesVariables(new WorkflowVariables());

        task = new Task("check-inventory", "checkInventoryTask", "shipping-route", null);
    }

    @Test
    void shouldCheckInventorySuccessfully() {
        // Given: Order with items in stock
        Order order = createValidOrder();
        context.getVariables().setValue("order", WorkflowVariable.WorkflowVariableType.OBJECT, order);

        when(inventoryService.checkInventoryAvailability(any(List.class))).thenReturn(true);
        when(inventoryService.reserveInventory(eq("ORDER-001"), any(List.class))).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = checkInventoryTask.executeTask(context, task);

        // Then: Inventory check should succeed
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getNextStep()).isEqualTo("shipping-route");
        assertThat(response.getMessage()).contains("Inventory checked and reserved");

        // Verify inventory was checked and reserved
        verify(inventoryService).checkInventoryAvailability(order.getItems());
        verify(inventoryService).reserveInventory("ORDER-001", order.getItems());

        // Verify order status was updated
        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.INVENTORY_CHECKED
        ));
    }

    @Test
    void shouldHandleInsufficientInventory() {
        // Given: Order with items not in stock
        Order order = createValidOrder();
        context.getVariables().setValue("order",WorkflowVariable.WorkflowVariableType.OBJECT, order);

        when(inventoryService.checkInventoryAvailability(any(List.class))).thenReturn(false);
        when(inventoryService.getAvailableQuantity("PROD-001")).thenReturn(0);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = checkInventoryTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INSUFFICIENT_INVENTORY");
        assertThat(response.getMessage()).contains("Insufficient inventory");

        // Verify inventory was NOT reserved
        verify(inventoryService, never()).reserveInventory(anyString(), any());

        // Verify order status was updated
        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.INVENTORY_INSUFFICIENT
        ));
    }

    @Test
    void shouldHandleInventoryReservationFailure() {
        // Given: Inventory available but reservation fails
        Order order = createValidOrder();
        context.getVariables().setValue("order", WorkflowVariable.WorkflowVariableType.OBJECT, order);

        when(inventoryService.checkInventoryAvailability(any(List.class))).thenReturn(true);
        when(inventoryService.reserveInventory(eq("ORDER-001"), any(List.class))).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = checkInventoryTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVENTORY_RESERVATION_FAILED");
        assertThat(response.getMessage()).contains("Failed to reserve inventory");
    }

    @Test
    void shouldHandleInventoryServiceException() {
        // Given: Inventory service throws exception
        Order order = createValidOrder();
        context.getVariables().setValue("order",WorkflowVariable.WorkflowVariableType.OBJECT ,order);

        when(inventoryService.checkInventoryAvailability(any(List.class)))
                .thenThrow(new RuntimeException("Inventory system unavailable"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = checkInventoryTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVENTORY_ERROR");
        assertThat(response.getMessage()).contains("Inventory system unavailable");
    }

    @Test
    void shouldHandleMissingOrderInContext() {
        // Given: Context without order

        // When: Task is executed
        TaskResponse response = checkInventoryTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVENTORY_ERROR");
        assertThat(response.getMessage()).contains("Order not found in workflow context");
    }

    private Order createValidOrder() {
        OrderItem item1 = OrderItem.builder()
                .productId("PROD-001")
                .productName("Laptop")
                .quantity(1)
                .unitPrice(new BigDecimal("999.99"))
                .build();

        OrderItem item2 = OrderItem.builder()
                .productId("PROD-002")
                .productName("Mouse")
                .quantity(2)
                .unitPrice(new BigDecimal("25.00"))
                .build();

        PaymentInfo paymentInfo = PaymentInfo.builder()
                .paymentMethod("CREDIT_CARD")
                .amount(new BigDecimal("1049.99"))
                .transactionId("TXN-12345")
                .paymentStatus("COMPLETED")
                .build();

        ShippingInfo shippingInfo = ShippingInfo.builder()
                .recipientName("John Doe")
                .addressLine1("123 Main St")
                .city("San Francisco")
                .state("CA")
                .postalCode("94102")
                .country("USA")
                .shippingMethod(ShippingMethod.STANDARD)
                .build();

        return Order.builder()
                .orderId("ORDER-001")
                .customerId("CUST-001")
                .customerEmail("john.doe@example.com")
                .items(Arrays.asList(item1, item2))
                .paymentInfo(paymentInfo)
                .shippingInfo(shippingInfo)
                .status(OrderStatus.PAYMENT_COMPLETED)
                .build();
    }
}
