package com.anode.workflow.example.tasks;

import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.model.OrderStatus;
import com.anode.workflow.example.service.InventoryService;
import com.anode.workflow.example.service.OrderRepository;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.service.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Task to check inventory availability and reserve items
 */
@Component("checkInventoryTask")
@Scope("prototype")
public class CheckInventoryTask implements InvokableTask {

    private static final Logger logger = LoggerFactory.getLogger(CheckInventoryTask.class);

    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final WorkflowContext context;

    public CheckInventoryTask(InventoryService inventoryService, OrderRepository orderRepository, WorkflowContext context) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.context = context;
    }

    @Override
    public TaskResponse executeStep() {
        logger.info("Checking inventory for case: {}", context.getCaseId());

        try {
            // Get order from context
            Order order = context.getProcessVariables().getObject("order");
            if (order == null) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Order not found in workflow context");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Check inventory availability
            boolean isAvailable = inventoryService.checkInventoryAvailability(order.getItems());

            if (!isAvailable) {
                order.setStatus(OrderStatus.INVENTORY_INSUFFICIENT);
                order.setUpdatedAt(LocalDateTime.now());
                order.setErrorMessage("Insufficient inventory for one or more items");
                orderRepository.save(order);

                logger.warn("Insufficient inventory for case: {}", context.getCaseId());
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Insufficient inventory for one or more items");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Reserve inventory
            boolean reserved = inventoryService.reserveInventory(order.getOrderId(), order.getItems());

            if (!reserved) {
                logger.error("Failed to reserve inventory for case: {}", context.getCaseId());
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Failed to reserve inventory");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Update order status
            order.setStatus(OrderStatus.INVENTORY_CHECKED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            logger.info("Inventory checked and reserved for case: {}", context.getCaseId());
            return new TaskResponse(StepResponseType.OK_PROCEED, "", "");

        } catch (Exception e) {
            logger.error("Error checking inventory for case: {}", context.getCaseId(), e);

            // Update order status on error
            Order order = context.getProcessVariables().getObject("order");
            if (order != null) {
                order.setStatus(OrderStatus.INVENTORY_INSUFFICIENT);
                order.setUpdatedAt(LocalDateTime.now());
                order.setErrorMessage(e.getMessage());
                orderRepository.save(order);
            }

            ErrorHandler error = new ErrorHandler();
            error.setErrorCode(1);
            error.setErrorMessage(e.getMessage());
            return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
        }
    }
}
