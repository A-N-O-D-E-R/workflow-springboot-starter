package com.anode.workflow.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shipping information for an order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingInfo {
    private String recipientName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private ShippingMethod shippingMethod;
    private String trackingNumber;
}
