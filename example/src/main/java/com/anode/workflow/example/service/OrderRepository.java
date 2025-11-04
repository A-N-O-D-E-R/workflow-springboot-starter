package com.anode.workflow.example.service;

import com.anode.workflow.example.model.Order;

import java.util.Optional;

/**
 * Repository for storing and retrieving orders
 */
public interface OrderRepository {

    /**
     * Save an order
     *
     * @param order Order to save
     * @return Saved order
     */
    Order save(Order order);

    /**
     * Find order by ID
     *
     * @param orderId Order ID
     * @return Order if found
     */
    Optional<Order> findById(String orderId);

    /**
     * Delete an order
     *
     * @param orderId Order ID
     */
    void deleteById(String orderId);

    /**
     * Check if order exists
     *
     * @param orderId Order ID
     * @return true if order exists
     */
    boolean existsById(String orderId);
}
