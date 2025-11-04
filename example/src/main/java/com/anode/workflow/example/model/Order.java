package com.anode.workflow.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Main order entity representing a customer order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String orderId;
    private String customerId;
    private String customerEmail;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private PaymentInfo paymentInfo;
    private ShippingInfo shippingInfo;

    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private String errorMessage;

    /**
     * Calculate total order amount
     */
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if order is valid for processing
     */
    public boolean isValid() {
        return orderId != null && !orderId.isEmpty()
                && customerId != null && !customerId.isEmpty()
                && customerEmail != null && !customerEmail.isEmpty()
                && items != null && !items.isEmpty()
                && paymentInfo != null
                && shippingInfo != null;
    }
}
