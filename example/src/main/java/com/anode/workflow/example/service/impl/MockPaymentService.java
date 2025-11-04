package com.anode.workflow.example.service.impl;

import com.anode.workflow.example.model.PaymentInfo;
import com.anode.workflow.example.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Mock payment service for demonstration
 */
@Service
public class MockPaymentService implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(MockPaymentService.class);

    @Override
    public PaymentInfo processPayment(PaymentInfo paymentInfo) {
        logger.info("Processing payment: Method={}, Amount={}",
                paymentInfo.getPaymentMethod(), paymentInfo.getAmount());

        // Simulate payment processing
        try {
            Thread.sleep(100); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generate transaction ID
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Update payment info
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setPaymentStatus("COMPLETED");

        logger.info("Payment processed successfully: TransactionId={}", transactionId);
        return paymentInfo;
    }

    @Override
    public boolean refundPayment(String transactionId) {
        logger.info("Refunding payment: TransactionId={}", transactionId);
        return true;
    }

    @Override
    public String verifyPaymentStatus(String transactionId) {
        logger.info("Verifying payment status: TransactionId={}", transactionId);
        return "COMPLETED";
    }
}
