package com.anode.workflow.example.routes;

import com.anode.workflow.entities.steps.responses.RouteResponse;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.example.model.Order;
import com.anode.workflow.example.model.ShippingMethod;
import com.anode.workflow.entities.steps.InvokableRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Route to determine next step based on shipping method
 */
@Component("shippingMethodRoute")
@Scope("prototype")
public class ShippingMethodRoute implements InvokableRoute {

    private static final Logger logger = LoggerFactory.getLogger(ShippingMethodRoute.class);
    private final WorkflowContext context;

    public ShippingMethodRoute(WorkflowContext context) {
        this.context = context;
    }

    @Override
    public RouteResponse executeRoute() {
        logger.info("Evaluating shipping method route for case: {}", context.getCaseId());

        try {
            // Get order from context
            Order order = context.getProcessVariables().getObject("order");
            if (order == null || order.getShippingInfo() == null || order.getShippingInfo().getShippingMethod() == null) {
                logger.warn("Order or shipping info not found, using default route for case: {}", context.getCaseId());
                List<String> branches = new ArrayList<>();
                branches.add("STANDARD");
                return new RouteResponse(StepResponseType.OK_PROCEED, branches, "");
            }

            ShippingMethod shippingMethod = order.getShippingInfo().getShippingMethod();

            // Route based on shipping method
            String branchName = switch (shippingMethod) {
                case EXPRESS -> {
                    logger.info("Routing to express shipment for case: {}", context.getCaseId());
                    yield "EXPRESS";
                }
                case OVERNIGHT -> {
                    logger.info("Routing to overnight shipment for case: {}", context.getCaseId());
                    yield "OVERNIGHT";
                }
                case STANDARD -> {
                    logger.info("Routing to standard shipment for case: {}", context.getCaseId());
                    yield "STANDARD";
                }
            };

            List<String> branches = new ArrayList<>();
            branches.add(branchName);
            return new RouteResponse(StepResponseType.OK_PROCEED, branches, "");

        } catch (Exception e) {
            logger.error("Error evaluating shipping method route for case: {}", context.getCaseId(), e);
            List<String> branches = new ArrayList<>();
            branches.add("STANDARD");
            return new RouteResponse(StepResponseType.OK_PROCEED, branches, "");
        }
    }
}
