package com.anode.workflow.example.service.impl;

import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.model.ShippingInfo;
import com.anode.workflow.example.model.ShippingMethod;
import com.anode.workflow.example.service.ShippingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Mock shipping service for demonstration
 */
@Service
public class MockShippingService implements ShippingService {

    private static final Logger logger = LoggerFactory.getLogger(MockShippingService.class);

    @Override
    public ShippingInfo createShipment(Order order) {
        logger.info("Creating shipment for order: {}", order.getOrderId());

        // Generate tracking number
        String trackingNumber = "TRACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ShippingInfo shippingInfo = order.getShippingInfo();
        shippingInfo.setTrackingNumber(trackingNumber);

        logger.info("Shipment created: TrackingNumber={}, Method={}",
                trackingNumber, shippingInfo.getShippingMethod());

        return shippingInfo;
    }

    @Override
    public String getShippingStatus(String trackingNumber) {
        logger.info("Getting shipping status: TrackingNumber={}", trackingNumber);
        return "IN_TRANSIT";
    }

    @Override
    public boolean cancelShipment(String trackingNumber) {
        logger.info("Cancelling shipment: TrackingNumber={}", trackingNumber);
        return true;
    }

    @Override
    public String calculateEstimatedDelivery(ShippingInfo shippingInfo) {
        LocalDate today = LocalDate.now();
        LocalDate estimatedDelivery = switch (shippingInfo.getShippingMethod()) {
            case OVERNIGHT -> today.plusDays(1);
            case EXPRESS -> today.plusDays(2);
            case STANDARD -> today.plusDays(5);
        };

        String deliveryDate = estimatedDelivery.format(DateTimeFormatter.ISO_LOCAL_DATE);
        logger.info("Estimated delivery: {} for method {}", deliveryDate, shippingInfo.getShippingMethod());

        return deliveryDate;
    }
}
