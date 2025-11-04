package com.anode.workflow.example.tasks;

import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowVariable;
import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.model.OrderStatus;
import com.anode.workflow.example.model.ShippingInfo;
import com.anode.workflow.example.service.OrderRepository;
import com.anode.workflow.example.service.ShippingService;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.service.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Task to prepare shipment for an order
 */
@Component("prepareShipmentTask")
@Scope("prototype")
public class PrepareShipmentTask implements InvokableTask {

    private static final Logger logger = LoggerFactory.getLogger(PrepareShipmentTask.class);

    private final ShippingService shippingService;
    private final OrderRepository orderRepository;
    private final WorkflowContext context;

    public PrepareShipmentTask(ShippingService shippingService, OrderRepository orderRepository, WorkflowContext context) {
        this.shippingService = shippingService;
        this.orderRepository = orderRepository;
        this.context = context;
    }

    @Override
    public TaskResponse executeStep() {
        logger.info("Preparing shipment for case: {}", context.getCaseId());

        try {
            // Get order from context
            Order order = context.getProcessVariables().getObject("order");
            if (order == null) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Order not found in workflow context");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Update order status
            order.setStatus(OrderStatus.PREPARING_SHIPMENT);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Create shipment
            ShippingInfo updatedShippingInfo = shippingService.createShipment(order);

            if (updatedShippingInfo == null || updatedShippingInfo.getTrackingNumber() == null) {
                logger.error("Failed to create shipment for case: {}", context.getCaseId());
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Failed to create shipment");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Calculate estimated delivery
            String estimatedDelivery = shippingService.calculateEstimatedDelivery(updatedShippingInfo);

            // Update order with shipping info
            order.setShippingInfo(updatedShippingInfo);
            order.setStatus(OrderStatus.SHIPPED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Store estimated delivery in context for notification task
            context.getProcessVariables().setValue("estimatedDelivery", WorkflowVariable.WorkflowVariableType.STRING, estimatedDelivery);

            logger.info("Shipment prepared successfully for case: {}. Tracking: {}",
                    context.getCaseId(), updatedShippingInfo.getTrackingNumber());

            return new TaskResponse(StepResponseType.OK_PROCEED, "", "");

        } catch (Exception e) {
            logger.error("Error preparing shipment for case: {}", context.getCaseId(), e);

            // Update order status on error
            Order order = context.getProcessVariables().getObject("order");
            if (order != null) {
                order.setErrorMessage(e.getMessage());
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            }

            ErrorHandler error = new ErrorHandler();
            error.setErrorCode(1);
            error.setErrorMessage(e.getMessage());
            return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
        }
    }
}
