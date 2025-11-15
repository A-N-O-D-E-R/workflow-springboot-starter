package com.anode.workflow.example.controller;

import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.service.OrderWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for order management using workflow engine.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderWorkflowService orderWorkflowService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order processedOrder = orderWorkflowService.processOrder(order);
        return ResponseEntity.ok(processedOrder);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<String> getOrderStatus(@PathVariable String orderId) {
        return ResponseEntity.ok("Order service is running. Use POST to create orders.");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order workflow service is healthy");
    }
}
