package com.anode.workflow.example.model;

/**
 * Represents the various states an order can be in during workflow processing
 */
public enum OrderStatus {
    CREATED,
    VALIDATED,
    PAYMENT_PROCESSING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    INVENTORY_CHECKED,
    INVENTORY_INSUFFICIENT,
    PREPARING_SHIPMENT,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    COMPLETED
}
