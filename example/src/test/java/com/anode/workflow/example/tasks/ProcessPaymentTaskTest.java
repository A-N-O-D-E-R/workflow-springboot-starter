package com.anode.workflow.example.tasks;

import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.example.model.*;
import com.anode.workflow.example.service.OrderRepository;
import com.anode.workflow.example.service.PaymentService;
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
class ProcessPaymentTaskTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private OrderRepository orderRepository;

    private ProcessPaymentTask processPaymentTask;

    private WorkflowContext context;
    private Task task;

    @BeforeEach
    void setUp() {
        processPaymentTask = new ProcessPaymentTask(paymentService, orderRepository);

        context = new WorkflowContext();
        context.setCaseId("ORDER-001");
        context.setVariables(new WorkflowVariables());

        task = new Task("process-payment", "processPaymentTask", "check-inventory", null);
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given: Valid order with payment info
        Order order = createValidOrder();
        context.getProcessVariables().setProcessVariables("order", order);

        PaymentInfo processedPayment = PaymentInfo.builder()
                .paymentMethod("CREDIT_CARD")
                .amount(order.getTotalAmount())
                .transactionId("TXN-12345")
                .paymentStatus("COMPLETED")
                .cardLastFourDigits("1234")
                .build();

        when(paymentService.processPayment(any(PaymentInfo.class))).thenReturn(processedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = processPaymentTask.executeTask(context, task);

        // Then: Payment should be processed successfully
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getNextStep()).isEqualTo("check-inventory");
        assertThat(response.getMessage()).contains("Payment processed successfully");

        // Verify payment service was called
        verify(paymentService).processPayment(any(PaymentInfo.class));

        // Verify order was updated with transaction ID and status
        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.PAYMENT_COMPLETED &&
                        savedOrder.getPaymentInfo().getTransactionId().equals("TXN-12345") &&
                        savedOrder.getPaymentInfo().getPaymentStatus().equals("COMPLETED")
        ));
    }

    @Test
    void shouldHandlePaymentFailure() {
        // Given: Order with payment info
        Order order = createValidOrder();
        context.getProcessVariables().setProcessVariables("order", order);

        PaymentInfo failedPayment = PaymentInfo.builder()
                .paymentMethod("CREDIT_CARD")
                .amount(order.getTotalAmount())
                .paymentStatus("FAILED")
                .cardLastFourDigits("1234")
                .build();

        when(paymentService.processPayment(any(PaymentInfo.class))).thenReturn(failedPayment);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = processPaymentTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("PAYMENT_FAILED");
        assertThat(response.getMessage()).contains("Payment processing failed");

        // Verify order status was updated
        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.PAYMENT_FAILED
        ));
    }

    @Test
    void shouldHandlePaymentServiceException() {
        // Given: Order with payment info
        Order order = createValidOrder();
        context.getProcessVariables().setProcessVariables("order", order);

        when(paymentService.processPayment(any(PaymentInfo.class)))
                .thenThrow(new RuntimeException("Payment gateway unavailable"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = processPaymentTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("PAYMENT_ERROR");
        assertThat(response.getMessage()).contains("Payment gateway unavailable");

        // Verify order status was updated
        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.PAYMENT_FAILED
        ));
    }

    @Test
    void shouldValidatePaymentAmount() {
        // Given: Order with payment amount mismatch
        Order order = createValidOrder();
        order.getPaymentInfo().setAmount(new BigDecimal("100.00")); // Different from order total
        context.getProcessVariables().setProcessVariables("order", order);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = processPaymentTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("PAYMENT_AMOUNT_MISMATCH");
        assertThat(response.getMessage()).contains("Payment amount does not match order total");

        verify(paymentService, never()).processPayment(any());
    }

    @Test
    void shouldHandleMissingOrderInContext() {
        // Given: Context without order

        // When: Task is executed
        TaskResponse response = processPaymentTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("PAYMENT_ERROR");
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
                .cardLastFourDigits("1234")
                .paymentStatus("PENDING")
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
                .status(OrderStatus.VALIDATED)
                .build();
    }
}
