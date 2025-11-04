package com.anode.workflow.example.service.impl;

import com.anode.workflow.example.model.OrderItem;
import com.anode.workflow.example.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock inventory service for demonstration
 */
@Service
public class MockInventoryService implements InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(MockInventoryService.class);

    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();
    private final Map<String, Integer> reserved = new ConcurrentHashMap<>();

    public MockInventoryService() {
        // Initialize with some stock
        inventory.put("PROD-001", 100);
        inventory.put("PROD-002", 200);
        inventory.put("PROD-003", 50);
    }

    @Override
    public boolean checkInventoryAvailability(List<OrderItem> items) {
        for (OrderItem item : items) {
            int available = inventory.getOrDefault(item.getProductId(), 0);
            int alreadyReserved = reserved.getOrDefault(item.getProductId(), 0);
            int actuallyAvailable = available - alreadyReserved;

            if (actuallyAvailable < item.getQuantity()) {
                logger.warn("Insufficient inventory: Product={}, Required={}, Available={}",
                        item.getProductId(), item.getQuantity(), actuallyAvailable);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean reserveInventory(String orderId, List<OrderItem> items) {
        logger.info("Reserving inventory for order: {}", orderId);

        for (OrderItem item : items) {
            int currentReserved = reserved.getOrDefault(item.getProductId(), 0);
            reserved.put(item.getProductId(), currentReserved + item.getQuantity());
            logger.debug("Reserved {} units of product {} for order {}",
                    item.getQuantity(), item.getProductId(), orderId);
        }

        return true;
    }

    @Override
    public boolean releaseInventory(String orderId) {
        logger.info("Releasing inventory for order: {}", orderId);
        // In real implementation, track which items were reserved for which order
        return true;
    }

    @Override
    public int getAvailableQuantity(String productId) {
        int total = inventory.getOrDefault(productId, 0);
        int reservedQty = reserved.getOrDefault(productId, 0);
        return total - reservedQty;
    }
}
