package com.anode.workflow.example.tasks;

import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.model.OrderStatus;
import com.anode.workflow.example.model.PaymentInfo;
import com.anode.workflow.example.service.OrderRepository;
import com.anode.workflow.example.service.PaymentService;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.service.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Task to process payment for an order
 */
@Component("processPaymentTask")
@Scope("prototype")
public class ProcessPaymentTask implements InvokableTask {

    private static final Logger logger = LoggerFactory.getLogger(ProcessPaymentTask.class);

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final WorkflowContext context;

    public ProcessPaymentTask(PaymentService paymentService, OrderRepository orderRepository, WorkflowContext context) {
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
        this.context = context;
    }

    @Override
    public TaskResponse executeStep() {
        logger.info("Processing payment for case: {}", context.getCaseId());

        try {
            // Get order from context
            Order order = context.getProcessVariables().getObject("order");
            if (order == null) {
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Order not found in workflow context");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Validate payment amount matches order total
            if (order.getPaymentInfo().getAmount().compareTo(order.getTotalAmount()) != 0) {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Payment amount does not match order total");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Update order status to processing
            order.setStatus(OrderStatus.PAYMENT_PROCESSING);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Process payment
            PaymentInfo processedPayment = paymentService.processPayment(order.getPaymentInfo());

            // Check payment status
            if ("FAILED".equals(processedPayment.getPaymentStatus())) {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                order.setUpdatedAt(LocalDateTime.now());
                order.setPaymentInfo(processedPayment);
                orderRepository.save(order);
                ErrorHandler error = new ErrorHandler();
                error.setErrorCode(1);
                error.setErrorMessage("Payment processing failed");
                return new TaskResponse(StepResponseType.ERROR_PEND, "", "", error);
            }

            // Update order with payment details
            order.setPaymentInfo(processedPayment);
            order.setStatus(OrderStatus.PAYMENT_COMPLETED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            logger.info("Payment processed successfully for case: {}", context.getCaseId());
            return new TaskResponse(StepResponseType.OK_PROCEED, "", "");

        } catch (Exception e) {
            logger.error("Error processing payment for case: {}", context.getCaseId(), e);

            // Update order status on error
            Order order = context.getProcessVariables().getObject("order");
            if (order != null) {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
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
