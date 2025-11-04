package com.anode.workflow.example.tasks;

import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.example.model.*;
import com.anode.workflow.example.service.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateOrderTaskTest {

    @Mock
    private OrderRepository orderRepository;

    private ValidateOrderTask validateOrderTask;

    private WorkflowContext context;
    private Task task;

    @BeforeEach
    void setUp() {
        validateOrderTask = new ValidateOrderTask(orderRepository);

        // Create workflow context
        context = new WorkflowContext();
        context.setCaseId("ORDER-001");
        context.setVariables(new WorkflowVariables());

        // Create task
        task = new Task("validate-order", "validateOrderTask", "process-payment", null);
    }

    @Test
    void shouldValidateValidOrder() {
        // Given: A valid order
        Order order = createValidOrder();
        context.getProcessVariables().setProcessVariables("order", order);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Task is executed
        TaskResponse response = validateOrderTask.executeTask(context, task);

        // Then: Validation should succeed
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getNextStep()).isEqualTo("process-payment");
        assertThat(response.getMessage()).contains("validation successful");

        // Verify order was updated and saved
        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.VALIDATED
        ));
    }

    @Test
    void shouldRejectOrderWithMissingOrderId() {
        // Given: Order without order ID
        Order order = createValidOrder();
        order.setOrderId(null);
        context.getProcessVariables().set("order", order);

        // When: Task is executed
        TaskResponse response = validateOrderTask.executeTask(context, task);

        // Then: Validation should fail
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getMessage()).contains("Order ID is required");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldRejectOrderWithMissingCustomerId() {
        // Given: Order without customer ID
        Order order = createValidOrder();
        order.setCustomerId(null);
        context.getProcessVariables().set("order", order);

        // When: Task is executed
        TaskResponse response = validateOrderTask.executeTask(context, task);

        // Then: Validation should fail
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getMessage()).contains("Customer ID is required");
    }

    @Test
    void shouldRejectOrderWithMissingEmail() {
        // Given: Order without customer email
        Order order = createValidOrder();
        order.setCustomerEmail(null);
        context.getProcessVariables().set("order", order);

        // When: Task is executed
        TaskResponse response = validateOrderTask.executeTask(context, task);

        // Then: Validation should fail
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getMessage()).contains("Customer email is required");
    }

    @Test
    void shouldRejectOrderWithInvalidEmail() {
        // Given: Order with invalid email format
        Order order = createValidOrder();
        order.setCustomerEmail("invalid-email");
        context.getProcessVariables().set("order", order);

        // When: Task is executed
        TaskResponse response = validateOrderTask.executeTask(context, task);

        // Then: Validation should fail
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getMessage()).contains("Invalid email format");
    }

    @Test
    void shouldRejectOrderWithEmptyItems() {
        // Given: Order with no items
        Order order = createValidOrder();
        order.getItems().clear();
        context.getProcessVariables().set("order", order);

        // When: Task is executed
        TaskResponse response = validateOrderTask.executeTask(context, task);

        // Then: Validation should fail
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getMessage()).contains("Order must contain at least one item");
    }

    @Test
    void shouldRejectOrderWithMissingPaymentInfo() {
        // Given: Order without payment info
        Order order = createValidOrder();
        order.setPaymentInfo(null);
        context.getProcessVariables().set("order", order);

        // When: Task is executed
        TaskResponse response = validateOrderTask.executeTask(context, task);

        // Then: Validation should fail
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getMessage()).contains("Payment information is required");
    }

    @Test
    void shouldRejectOrderWithMissingShippingInfo() {
        // Given: Order without shipping info
        Order order = createValidOrder();
        order.setShippingInfo(null);
        context.getProcessVariables().set("order", order);

        // When: Task is executed
        TaskResponse response = validateOrderTask.executeTask(context, task);

        // Then: Validation should fail
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getMessage()).contains("Shipping information is required");
    }

    @Test
    void shouldRejectOrderWithZeroTotalAmount() {
        // Given: Order with items having zero price
        Order order = createValidOrder();
        order.getItems().forEach(item -> item.setUnitPrice(BigDecimal.ZERO));
        context.getProcessVariables().set("order", order);

        // When: Task is executed
        TaskResponse response = validateOrderTask.executeTask(context, task);

        // Then: Validation should fail
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getMessage()).contains("Order total must be greater than zero");
    }

    @Test
    void shouldHandleMissingOrderInContext() {
        // Given: Context without order
        // (context is already created without order)

        // When: Task is executed
        TaskResponse response = validateOrderTask.executeTask(context, task);

        // Then: Should return error
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_FAILED");
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
                .status(OrderStatus.CREATED)
                .build();
    }
}
