package com.anode.workflow.example.controller;

import com.anode.workflow.example.model.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request object for order submission
 */
@Data
public class OrderRequest {
    private String orderId;
    private String customerId;
    private String customerEmail;
    private List<OrderItem> items;
    private String paymentMethod;
    private PaymentInfoRequest paymentInfo;
    private ShippingInfoRequest shippingInfo;
    private boolean useRouting = true; // Use routing workflow by default

    @Data
    public static class PaymentInfoRequest {
        private BigDecimal amount;
        private String cardLastFourDigits;
    }

    @Data
    public static class ShippingInfoRequest {
        private String recipientName;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String shippingMethod; // STANDARD, EXPRESS, OVERNIGHT
    }
}
