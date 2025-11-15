package com.anode.workflow.example.complex.route;

import com.anode.workflow.entities.steps.InvokableRoute;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.example.complex.model.OrderRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Routes workflow based on customer type for specialized handling.
 */
@Slf4j
@Task
public class CustomerTypeRoute implements InvokableRoute {

    @Override
    public String executeRoute() {
        log.info("==> Executing CustomerTypeRoute");

        OrderRequest order = (OrderRequest) getWorkflowContext().getVariables().getValue("order");

        String nextStep = switch (order.getCustomerType()) {
            case VIP -> {
                log.info("Routing to VIP processing");
                yield "applyDiscountTask";  // VIPs get priority discount processing
            }
            case CORPORATE -> {
                log.info("Routing to Corporate processing");
                yield "applyDiscountTask";  // Corporate also gets discounts
            }
            case NEW -> {
                log.info("Routing to new customer processing");
                yield "checkInventoryTask";  // New customers go straight to inventory check
            }
            default -> {
                log.info("Routing to regular processing");
                yield "checkInventoryTask";
            }
        };

        log.info("Next step: {}", nextStep);
        return nextStep;
    }
}
