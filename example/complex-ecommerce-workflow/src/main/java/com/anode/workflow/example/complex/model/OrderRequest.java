package com.anode.workflow.example.complex.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a complex e-commerce order with multiple items and customer information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String orderId;
    private String customerId;
    private String customerEmail;
    private CustomerType customerType;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String billingAddress;
    private PaymentMethod paymentMethod;
    private boolean requiresGiftWrap;
    private boolean isInternational;
    private String promoCode;

    public enum CustomerType {
        NEW, REGULAR, VIP, CORPORATE
    }

    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER, CRYPTOCURRENCY
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private boolean inStock;
        private String warehouseLocation;
    }
}
