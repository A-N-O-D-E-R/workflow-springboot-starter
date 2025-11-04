package com.anode.workflow.example.service.impl;

import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.service.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of OrderRepository for demo purposes
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        orders.put(order.getOrderId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public void deleteById(String orderId) {
        orders.remove(orderId);
    }

    @Override
    public boolean existsById(String orderId) {
        return orders.containsKey(orderId);
    }
}
