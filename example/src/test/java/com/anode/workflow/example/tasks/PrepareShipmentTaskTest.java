package com.anode.workflow.example.tasks;

import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.example.model.*;
import com.anode.workflow.example.service.OrderRepository;
import com.anode.workflow.example.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrepareShipmentTaskTest {

    @Mock
    private ShippingService shippingService;

    @Mock
    private OrderRepository orderRepository;

    private PrepareShipmentTask prepareShipmentTask;

    private WorkflowContext context;
    private Task task;

    @BeforeEach
    void setUp() {
        prepareShipmentTask = new PrepareShipmentTask(shippingService, orderRepository);

        context = new WorkflowContext();
        context.setCaseId("ORDER-001");
        context.setVariables(new WorkflowVariables());

        task = new Task("prepare-shipment", "prepareShipmentTask", "send-notification", null);
    }

    @Test
    void shouldPrepareShipmentSuccessfully() {
        // Given: Order ready for shipment
        Order order = createValidOrder();
        context.getProcessVariables().setProcessVariables("order", order);

        ShippingInfo updatedShippingInfo = ShippingInfo.builder()
                .recipientName("John Doe")
                .addressLine1("123 Main St")
                .city("San Francisco")
                .state("CA")
                .postalCode("94102")
                .country("USA")
                .shippingMethod(ShippingMethod.STANDARD)
                .trackingNumber("TRACK-12345")
                .build();

        when(shippingService.createShipment(any(Order.class))).thenReturn(updatedShippingInfo);
        when(shippingService.calculateEstimatedDelivery(any(ShippingInfo.class))).thenReturn("2025-01-15");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = prepareShipmentTask.executeTask(context, task);

        // Then: Shipment should be prepared successfully
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getNextStep()).isEqualTo("send-notification");
        assertThat(response.getMessage()).contains("Shipment prepared successfully");

        // Verify shipping service was called
        verify(shippingService).createShipment(order);
        verify(shippingService).calculateEstimatedDelivery(any(ShippingInfo.class));

        // Verify order was updated with tracking number and status
        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.SHIPPED &&
                        savedOrder.getShippingInfo().getTrackingNumber().equals("TRACK-12345")
        ));

        // Verify estimated delivery was added to context
        assertThat(context.getProcessVariables().get("estimatedDelivery")).isEqualTo("2025-01-15");
    }

    @Test
    void shouldHandleShipmentCreationFailure() {
        // Given: Order ready for shipment but service fails
        Order order = createValidOrder();
        context.getProcessVariables().setProcessVariables("order", order);

        when(shippingService.createShipment(any(Order.class))).thenReturn(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = prepareShipmentTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("SHIPMENT_CREATION_FAILED");
        assertThat(response.getMessage()).contains("Failed to create shipment");
    }

    @Test
    void shouldHandleShippingServiceException() {
        // Given: Shipping service throws exception
        Order order = createValidOrder();
        context.getProcessVariables().setProcessVariables("order", order);

        when(shippingService.createShipment(any(Order.class)))
                .thenThrow(new RuntimeException("Shipping provider unavailable"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = prepareShipmentTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("SHIPMENT_ERROR");
        assertThat(response.getMessage()).contains("Shipping provider unavailable");
    }

    @Test
    void shouldHandleExpressShipping() {
        // Given: Order with express shipping
        Order order = createValidOrder();
        order.getShippingInfo().setShippingMethod(ShippingMethod.EXPRESS);
        context.getProcessVariables().setProcessVariables("order", order);

        ShippingInfo updatedShippingInfo = ShippingInfo.builder()
                .recipientName("John Doe")
                .addressLine1("123 Main St")
                .city("San Francisco")
                .state("CA")
                .postalCode("94102")
                .country("USA")
                .shippingMethod(ShippingMethod.EXPRESS)
                .trackingNumber("TRACK-12345")
                .build();

        when(shippingService.createShipment(any(Order.class))).thenReturn(updatedShippingInfo);
        when(shippingService.calculateEstimatedDelivery(any(ShippingInfo.class))).thenReturn("2025-01-10");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = prepareShipmentTask.executeTask(context, task);

        // Then: Should succeed with express shipping
        assertThat(response.isSuccess()).isTrue();
        verify(shippingService).createShipment(order);
    }

    @Test
    void shouldHandleMissingOrderInContext() {
        // Given: Context without order

        // When: Task is executed
        TaskResponse response = prepareShipmentTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("SHIPMENT_ERROR");
        assertThat(response.getMessage()).contains("Order not found in workflow context");
    }

    private Order createValidOrder() {
        OrderItem item1 = OrderItem.builder()
                .productId("PROD-001")
                .productName("Laptop")
                .quantity(1)
                .unitPrice(new BigDecimal("999.99"))
                .build();

        PaymentInfo paymentInfo = PaymentInfo.builder()
                .paymentMethod("CREDIT_CARD")
                .amount(new BigDecimal("999.99"))
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
                .items(Arrays.asList(item1))
                .paymentInfo(paymentInfo)
                .shippingInfo(shippingInfo)
                .status(OrderStatus.INVENTORY_CHECKED)
                .build();
    }
}
