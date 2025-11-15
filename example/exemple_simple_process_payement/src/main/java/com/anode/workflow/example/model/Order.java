package com.anode.workflow.example.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Simple order model for the example workflow.
 */
@Data
public class Order implements Serializable {
    private String orderId;
    private String customerName;
    private Double amount;
    private String status;
}
