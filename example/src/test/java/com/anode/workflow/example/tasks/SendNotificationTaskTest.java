package com.anode.workflow.example.tasks;

import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.example.model.*;
import com.anode.workflow.example.service.NotificationService;
import com.anode.workflow.example.service.OrderRepository;
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
class SendNotificationTaskTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private OrderRepository orderRepository;

    private SendNotificationTask sendNotificationTask;

    private WorkflowContext context;
    private Task task;

    @BeforeEach
    void setUp() {
        sendNotificationTask = new SendNotificationTask(notificationService, orderRepository);

        context = new WorkflowContext();
        context.setCaseId("ORDER-001");
        context.setVariables(new WorkflowVariables());

        task = new Task("send-notification", "sendNotificationTask", null, null);
    }

    @Test
    void shouldSendNotificationSuccessfully() {
        // Given: Order with shipping info
        Order order = createValidOrder();
        context.getProcessVariables().setProcessVariables("order", order);

        doNothing().when(notificationService).sendShippingNotification(any(Order.class));
        doNothing().when(notificationService).sendOrderCompletionNotification(any(Order.class));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = sendNotificationTask.executeTask(context, task);

        // Then: Notifications should be sent
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getNextStep()).isNull(); // End of workflow
        assertThat(response.getMessage()).contains("Notifications sent successfully");

        // Verify notifications were sent
        verify(notificationService).sendShippingNotification(order);
        verify(notificationService).sendOrderCompletionNotification(order);

        // Verify order status was updated
        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.COMPLETED
        ));
    }

    @Test
    void shouldHandleNotificationServiceException() {
        // Given: Notification service throws exception
        Order order = createValidOrder();
        context.getProcessVariables().setProcessVariables("order", order);

        doThrow(new RuntimeException("Email service unavailable"))
                .when(notificationService).sendShippingNotification(any(Order.class));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = sendNotificationTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("NOTIFICATION_ERROR");
        assertThat(response.getMessage()).contains("Email service unavailable");
    }

    @Test
    void shouldHandleMissingOrderInContext() {
        // Given: Context without order

        // When: Task is executed
        TaskResponse response = sendNotificationTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("NOTIFICATION_ERROR");
        assertThat(response.getMessage()).contains("Order not found in workflow context");
    }

    @Test
    void shouldSendNotificationWithEstimatedDelivery() {
        // Given: Order with estimated delivery in context
        Order order = createValidOrder();
        context.getProcessVariables().setProcessVariables("order", order);
        context.getProcessVariables().setProcessVariables("estimatedDelivery", "2025-01-15");

        doNothing().when(notificationService).sendShippingNotification(any(Order.class));
        doNothing().when(notificationService).sendOrderCompletionNotification(any(Order.class));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = sendNotificationTask.executeTask(context, task);

        // Then: Should succeed
        assertThat(response.isSuccess()).isTrue();
        verify(notificationService).sendShippingNotification(order);
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
                .trackingNumber("TRACK-12345")
                .build();

        return Order.builder()
                .orderId("ORDER-001")
                .customerId("CUST-001")
                .customerEmail("john.doe@example.com")
                .items(Arrays.asList(item1))
                .paymentInfo(paymentInfo)
                .shippingInfo(shippingInfo)
                .status(OrderStatus.SHIPPED)
                .build();
    }
}
