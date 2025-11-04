package com.anode.workflow.example.service;

import com.anode.workflow.example.model.OrderItem;

import java.util.List;

/**
 * Service for managing inventory
 */
public interface InventoryService {

    /**
     * Check if all items are available in inventory
     *
     * @param items List of order items
     * @return true if all items are available in requested quantities
     */
    boolean checkInventoryAvailability(List<OrderItem> items);

    /**
     * Reserve inventory for order items
     *
     * @param orderId Order ID for reservation
     * @param items   List of items to reserve
     * @return true if reservation successful
     */
    boolean reserveInventory(String orderId, List<OrderItem> items);

    /**
     * Release reserved inventory (e.g., when order is cancelled)
     *
     * @param orderId Order ID
     * @return true if release successful
     */
    boolean releaseInventory(String orderId);

    /**
     * Get available quantity for a product
     *
     * @param productId Product ID
     * @return Available quantity
     */
    int getAvailableQuantity(String productId);
}
