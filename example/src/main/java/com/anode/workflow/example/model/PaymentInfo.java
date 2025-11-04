package com.anode.workflow.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payment information for an order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {
    private String paymentMethod; // CREDIT_CARD, PAYPAL, BANK_TRANSFER
    private String transactionId;
    private BigDecimal amount;
    private String cardLastFourDigits;
    private String paymentStatus; // PENDING, COMPLETED, FAILED
}
