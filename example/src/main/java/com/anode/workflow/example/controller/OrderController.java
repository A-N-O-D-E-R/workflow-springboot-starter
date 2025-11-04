package com.anode.workflow.example.controller;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariable;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.example.model.*;
import com.anode.workflow.example.service.OrderRepository;
import com.anode.workflow.example.workflow.OrderProcessingWorkflow;
import com.anode.workflow.service.runtime.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for order workflow operations
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private OrderProcessingWorkflow orderProcessingWorkflow;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Submit a new order for processing
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> submitOrder(@RequestBody OrderRequest orderRequest) {
        logger.info("Received order submission request: {}", orderRequest.getOrderId());

        try {
            // Create order from request
            Order order = buildOrderFromRequest(orderRequest);

            // Save order
            orderRepository.save(order);

            // Create workflow definition
            WorkflowDefinition workflow = orderProcessingWorkflow.createOrderWorkflow();

            // Create workflow variables
            WorkflowVariables variables = new WorkflowVariables();
            variables.setValue("order", WorkflowVariable.WorkflowVariableType.OBJECT, order);
            variables.setValue("customerId", WorkflowVariable.WorkflowVariableType.STRING, order.getCustomerId());
            variables.setValue("orderTotal", WorkflowVariable.WorkflowVariableType.STRING, order.getTotalAmount().toString());

            // Start workflow
            WorkflowContext context = runtimeService.startCase(
                    order.getOrderId(),
                    workflow,
                    variables,
                    null
            );

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.getOrderId());
            response.put("status", order.getStatus());
            response.put("message", "Order submitted successfully");

            logger.info("Order workflow started: OrderId={}",
                    order.getOrderId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Error processing order submission", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get order status
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        logger.info("Getting order: {}", orderId);

        return orderRepository.findById(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Resume a paused workflow (for demonstration)
     */
    @PostMapping("/{orderId}/resume")
    public ResponseEntity<Map<String, Object>> resumeOrder(@PathVariable String orderId) {
        logger.info("Resuming workflow for order: {}", orderId);

        try {
            runtimeService.resumeCase(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Workflow resumed successfully");
            response.put("orderId", orderId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error resuming workflow for order: {}", orderId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cancel an order
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderId) {
        logger.info("Cancelling order: {}", orderId);

        return orderRepository.findById(orderId)
                .map(order -> {
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);

                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Order cancelled successfully");
                    response.put("orderId", orderId);

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Order buildOrderFromRequest(OrderRequest request) {
        // Build payment info
        PaymentInfo paymentInfo = PaymentInfo.builder()
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getPaymentInfo().getAmount())
                .cardLastFourDigits(request.getPaymentInfo().getCardLastFourDigits())
                .paymentStatus("PENDING")
                .build();

        // Build shipping info
        ShippingInfo shippingInfo = ShippingInfo.builder()
                .recipientName(request.getShippingInfo().getRecipientName())
                .addressLine1(request.getShippingInfo().getAddressLine1())
                .addressLine2(request.getShippingInfo().getAddressLine2())
                .city(request.getShippingInfo().getCity())
                .state(request.getShippingInfo().getState())
                .postalCode(request.getShippingInfo().getPostalCode())
                .country(request.getShippingInfo().getCountry())
                .shippingMethod(ShippingMethod.valueOf(request.getShippingInfo().getShippingMethod()))
                .build();

        // Build order
        return Order.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .items(request.getItems())
                .paymentInfo(paymentInfo)
                .shippingInfo(shippingInfo)
                .status(OrderStatus.CREATED)
                .build();
    }
}
