package com.anode.workflow.example.service;

import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.model.ShippingInfo;

/**
 * Service for managing shipments
 */
public interface ShippingService {

    /**
     * Create a shipment for an order
     *
     * @param order Order to ship
     * @return Updated shipping info with tracking number
     */
    ShippingInfo createShipment(Order order);

    /**
     * Get shipping status
     *
     * @param trackingNumber Tracking number
     * @return Shipping status
     */
    String getShippingStatus(String trackingNumber);

    /**
     * Cancel a shipment
     *
     * @param trackingNumber Tracking number
     * @return true if cancellation successful
     */
    boolean cancelShipment(String trackingNumber);

    /**
     * Calculate estimated delivery date based on shipping method
     *
     * @param shippingInfo Shipping information
     * @return Estimated delivery date in ISO format
     */
    String calculateEstimatedDelivery(ShippingInfo shippingInfo);
}
