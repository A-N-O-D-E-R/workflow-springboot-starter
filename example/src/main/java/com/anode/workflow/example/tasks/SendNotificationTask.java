package com.anode.workflow.example.tasks;

import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.model.OrderStatus;
import com.anode.workflow.example.service.NotificationService;
import com.anode.workflow.example.service.OrderRepository;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.service.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Task to send notifications to customer
 */
@Component("sendNotificationTask")
@Scope("prototype")
public class SendNotificationTask implements InvokableTask {

    private static final Logger logger = LoggerFactory.getLogger(SendNotificationTask.class);

    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final WorkflowContext context;

    public SendNotificationTask(NotificationService notificationService, OrderRepository orderRepository, WorkflowContext context) {
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
        this.context = context;
    }

    @Override
    public TaskResponse executeStep() {
        logger.info("Sending notifications for case: {}", context.getCaseId());

        try {
            // Get order from context
            Order order = context.getProcessVariables().getObject("order");
            if (order == null) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Order not found in workflow context");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Send shipping notification
            notificationService.sendShippingNotification(order);
            logger.info("Shipping notification sent for case: {}", context.getCaseId());

            // Send order completion notification
            notificationService.sendOrderCompletionNotification(order);
            logger.info("Order completion notification sent for case: {}", context.getCaseId());

            // Update order status to completed
            order.setStatus(OrderStatus.COMPLETED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            return new TaskResponse(StepResponseType.OK_PROCEED, "", "");

        } catch (Exception e) {
            logger.error("Error sending notifications for case: {}", context.getCaseId(), e);

            // Note: We don't update order status on notification failure
            // as the order processing is complete

            ErrorHandler error = new ErrorHandler();
            error.setErrorCode(1);
            error.setErrorMessage(e.getMessage());
            return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
        }
    }
}
