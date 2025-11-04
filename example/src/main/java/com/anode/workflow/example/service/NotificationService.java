package com.anode.workflow.example.service;

import com.anode.workflow.example.model.Order;

/**
 * Service for sending notifications
 */
public interface NotificationService {

    /**
     * Send order confirmation email
     *
     * @param order Order details
     */
    void sendOrderConfirmation(Order order);

    /**
     * Send payment confirmation email
     *
     * @param order Order details
     */
    void sendPaymentConfirmation(Order order);

    /**
     * Send shipping notification email
     *
     * @param order Order details
     */
    void sendShippingNotification(Order order);

    /**
     * Send order completion email
     *
     * @param order Order details
     */
    void sendOrderCompletionNotification(Order order);

    /**
     * Send error notification
     *
     * @param order        Order details
     * @param errorMessage Error message
     */
    void sendErrorNotification(Order order, String errorMessage);
}
