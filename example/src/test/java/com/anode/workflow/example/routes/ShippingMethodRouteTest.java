package com.anode.workflow.example.routes;

import com.anode.workflow.entities.steps.Route;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.example.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingMethodRouteTest {

    private ShippingMethodRoute shippingMethodRoute;
    private WorkflowContext context;
    private Route route;

    @BeforeEach
    void setUp() {
        shippingMethodRoute = new ShippingMethodRoute();

        context = new WorkflowContext();
        context.setCaseId("ORDER-001");
        context.setVariables(new WorkflowVariables());

        // Create route with different possible next steps
        route = new Route(
                "shipping-method-route",
                "shippingMethodRoute",
                "prepare-shipment",  // default/standard
                null
        );
    }

    @Test
    void shouldRouteToStandardShipmentPreparation() {
        // Given: Order with standard shipping
        Order order = createOrder(ShippingMethod.STANDARD);
        context.getProcessVariables().setProcessVariables("order", order);

        // When: Route is evaluated
        String nextStep = shippingMethodRoute.getRoute(context, route);

        // Then: Should route to standard shipment preparation
        assertThat(nextStep).isEqualTo("prepare-shipment");
    }

    @Test
    void shouldRouteToExpressShipmentPreparation() {
        // Given: Order with express shipping
        Order order = createOrder(ShippingMethod.EXPRESS);
        context.getProcessVariables().setProcessVariables("order", order);

        // When: Route is evaluated
        String nextStep = shippingMethodRoute.getRoute(context, route);

        // Then: Should route to express shipment preparation
        assertThat(nextStep).isEqualTo("prepare-express-shipment");
    }

    @Test
    void shouldRouteToOvernightShipmentPreparation() {
        // Given: Order with overnight shipping
        Order order = createOrder(ShippingMethod.OVERNIGHT);
        context.getProcessVariables().setProcessVariables("order", order);

        // When: Route is evaluated
        String nextStep = shippingMethodRoute.getRoute(context, route);

        // Then: Should route to overnight shipment preparation
        assertThat(nextStep).isEqualTo("prepare-overnight-shipment");
    }

    @Test
    void shouldUseDefaultRouteWhenOrderMissing() {
        // Given: Context without order

        // When: Route is evaluated
        String nextStep = shippingMethodRoute.getRoute(context, route);

        // Then: Should use default route
        assertThat(nextStep).isEqualTo("prepare-shipment");
    }

    @Test
    void shouldUseDefaultRouteWhenShippingInfoMissing() {
        // Given: Order without shipping info
        Order order = createOrder(ShippingMethod.STANDARD);
        order.setShippingInfo(null);
        context.getProcessVariables().setProcessVariables("order", order);

        // When: Route is evaluated
        String nextStep = shippingMethodRoute.getRoute(context, route);

        // Then: Should use default route
        assertThat(nextStep).isEqualTo("prepare-shipment");
    }

    @Test
    void shouldUseDefaultRouteWhenShippingMethodMissing() {
        // Given: Order with shipping info but no method
        Order order = createOrder(ShippingMethod.STANDARD);
        order.getShippingInfo().setShippingMethod(null);
        context.getProcessVariables().setProcessVariables("order", order);

        // When: Route is evaluated
        String nextStep = shippingMethodRoute.getRoute(context, route);

        // Then: Should use default route
        assertThat(nextStep).isEqualTo("prepare-shipment");
    }

    private Order createOrder(ShippingMethod shippingMethod) {
        OrderItem item = OrderItem.builder()
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
                .shippingMethod(shippingMethod)
                .build();

        return Order.builder()
                .orderId("ORDER-001")
                .customerId("CUST-001")
                .customerEmail("john.doe@example.com")
                .items(Arrays.asList(item))
                .paymentInfo(paymentInfo)
                .shippingInfo(shippingInfo)
                .status(OrderStatus.INVENTORY_CHECKED)
                .build();
    }
}
