package com.anode.workflow.example.complex.controller;

import com.anode.workflow.example.complex.model.OrderRequest;
import com.anode.workflow.example.complex.service.ComplexOrderWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for complex order processing.
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final ComplexOrderWorkflowService workflowService;

    @PostMapping
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody OrderRequest order) {
        log.info("Received order creation request for customer: {}", order.getCustomerId());

        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        }

        // Execute complex workflow
        workflowService.processComplexOrder(order);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "orderId", order.getOrderId(),
            "message", "Order processed through complex workflow"
        ));
    }

    /**
     * Create a sample VIP order for testing.
     */
    @PostMapping("/sample/vip")
    public ResponseEntity<Map<String, String>> createSampleVIPOrder() {
        OrderRequest order = createSampleOrder(OrderRequest.CustomerType.VIP);
        workflowService.processComplexOrder(order);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "orderId", order.getOrderId(),
            "customerType", "VIP",
            "message", "VIP order processed with priority"
        ));
    }

    /**
     * Create a sample corporate order for testing.
     */
    @PostMapping("/sample/corporate")
    public ResponseEntity<Map<String, String>> createSampleCorporateOrder() {
        OrderRequest order = createSampleOrder(OrderRequest.CustomerType.CORPORATE);
        workflowService.processComplexOrder(order);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "orderId", order.getOrderId(),
            "customerType", "CORPORATE",
            "message", "Corporate order processed with bulk discount"
        ));
    }

    /**
     * Create a sample international order for testing.
     */
    @PostMapping("/sample/international")
    public ResponseEntity<Map<String, String>> createSampleInternationalOrder() {
        OrderRequest order = createSampleOrder(OrderRequest.CustomerType.REGULAR);
        order.setInternational(true);
        order.setShippingAddress("123 International St, London, UK");

        workflowService.processComplexOrder(order);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "orderId", order.getOrderId(),
            "type", "INTERNATIONAL",
            "message", "International order processed"
        ));
    }

    private OrderRequest createSampleOrder(OrderRequest.CustomerType customerType) {
        OrderRequest order = new OrderRequest();
        order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        order.setCustomerId("CUST-" + UUID.randomUUID().toString().substring(0, 8));
        order.setCustomerEmail("customer@example.com");
        order.setCustomerType(customerType);

        // Create sample items
        List<OrderRequest.OrderItem> items = List.of(
            new OrderRequest.OrderItem("PROD-001", "Laptop", 1, new BigDecimal("1200.00"), true, "Warehouse-A"),
            new OrderRequest.OrderItem("PROD-002", "Mouse", 2, new BigDecimal("25.00"), true, "Warehouse-B"),
            new OrderRequest.OrderItem("PROD-003", "Keyboard", 1, new BigDecimal("75.00"), true, "Warehouse-A")
        );

        order.setItems(items);
        order.setTotalAmount(new BigDecimal("1325.00"));
        order.setShippingAddress("123 Main St, City, Country");
        order.setBillingAddress("123 Main St, City, Country");
        order.setPaymentMethod(OrderRequest.PaymentMethod.CREDIT_CARD);
        order.setRequiresGiftWrap(false);
        order.setInternational(false);

        return order;
    }
}
