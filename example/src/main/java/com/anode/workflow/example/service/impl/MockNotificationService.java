package com.anode.workflow.example.service.impl;

import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mock notification service for demonstration
 */
@Service
public class MockNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(MockNotificationService.class);

    @Override
    public void sendOrderConfirmation(Order order) {
        logger.info("Sending order confirmation email to: {} for order: {}",
                order.getCustomerEmail(), order.getOrderId());
        logger.info("Order details: {} items, Total: ${}",
                order.getItems().size(), order.getTotalAmount());
    }

    @Override
    public void sendPaymentConfirmation(Order order) {
        logger.info("Sending payment confirmation email to: {} for order: {}",
                order.getCustomerEmail(), order.getOrderId());
        logger.info("Payment transaction: {}", order.getPaymentInfo().getTransactionId());
    }

    @Override
    public void sendShippingNotification(Order order) {
        logger.info("Sending shipping notification email to: {} for order: {}",
                order.getCustomerEmail(), order.getOrderId());
        logger.info("Tracking number: {}", order.getShippingInfo().getTrackingNumber());
    }

    @Override
    public void sendOrderCompletionNotification(Order order) {
        logger.info("Sending order completion notification email to: {} for order: {}",
                order.getCustomerEmail(), order.getOrderId());
        logger.info("Order status: {}", order.getStatus());
    }

    @Override
    public void sendErrorNotification(Order order, String errorMessage) {
        logger.error("Sending error notification email to: {} for order: {}",
                order.getCustomerEmail(), order.getOrderId());
        logger.error("Error message: {}", errorMessage);
    }
}
