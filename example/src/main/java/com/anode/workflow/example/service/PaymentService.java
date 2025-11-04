package com.anode.workflow.example.service;

import com.anode.workflow.example.model.PaymentInfo;

/**
 * Service for processing payments
 */
public interface PaymentService {

    /**
     * Process a payment transaction
     *
     * @param paymentInfo Payment details
     * @return Updated payment info with transaction ID and status
     */
    PaymentInfo processPayment(PaymentInfo paymentInfo);

    /**
     * Refund a payment
     *
     * @param transactionId Original transaction ID
     * @return true if refund successful
     */
    boolean refundPayment(String transactionId);

    /**
     * Verify payment status
     *
     * @param transactionId Transaction ID to verify
     * @return Payment status
     */
    String verifyPaymentStatus(String transactionId);
}
