package com.anode.workflow.example.tasks;

import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.model.OrderStatus;
import com.anode.workflow.example.service.OrderRepository;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.service.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Task to validate order before processing
 */
@Component("validateOrderTask")
@Scope("prototype")
public class ValidateOrderTask implements InvokableTask {

    private static final Logger logger = LoggerFactory.getLogger(ValidateOrderTask.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final OrderRepository orderRepository;
    private final WorkflowContext context;

    public ValidateOrderTask(OrderRepository orderRepository, WorkflowContext context) {
        this.orderRepository = orderRepository;
        this.context = context;
    }

    @Override
    public TaskResponse executeStep() {
        logger.info("Validating order for case: {}", context.getCaseId());

        try {
            // Get order from context
            Order order = context.getProcessVariables().getObject("order");
            if (order == null) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Order not found in workflow context");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Validate order ID
            if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Order ID is required");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Validate customer ID
            if (order.getCustomerId() == null || order.getCustomerId().isEmpty()) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Customer ID is required");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Validate customer email
            if (order.getCustomerEmail() == null || order.getCustomerEmail().isEmpty()) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Customer email is required");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            if (!EMAIL_PATTERN.matcher(order.getCustomerEmail()).matches()) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Invalid email format");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Validate items
            if (order.getItems() == null || order.getItems().isEmpty()) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Order must contain at least one item");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Validate payment info
            if (order.getPaymentInfo() == null) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Payment information is required");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Validate shipping info
            if (order.getShippingInfo() == null) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Shipping information is required");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Validate total amount
            BigDecimal totalAmount = order.getTotalAmount();
            if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Order total must be greater than zero");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Update order status
            order.setStatus(OrderStatus.VALIDATED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            logger.info("Order validation successful for case: {}", context.getCaseId());
            return new TaskResponse(StepResponseType.OK_PROCEED, "", "");

        } catch (Exception e) {
            logger.error("Error validating order for case: {}", context.getCaseId(), e);
            ErrorHandler error = new ErrorHandler();
            error.setErrorCode(1);
            error.setErrorMessage("Error validating order: " + e.getMessage());
            return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
        }
    }
}
