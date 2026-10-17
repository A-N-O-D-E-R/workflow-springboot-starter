package com.anode.workflow.spring.autoconfigure.impl;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Service;

import com.anode.workflow.spring.autoconfigure.runtime.ContextualRoute;
import com.anode.workflow.spring.autoconfigure.runtime.ContextualTask;

import com.anode.workflow.entities.steps.InvokableRoute;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.Step.StepType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.WorkflowComponantFactory;


/**
 * Default implementation of workflow component factory.
 *
 * <p>This factory looks up workflow components (tasks and routes) from the Spring
 * context using ObjectProvider. Components are cached after initialization for
 * fast lookup during workflow execution.
 *
 * <p>Supported component types:
 * <ul>
 *   <li><b>TASK:</b> Returns InvokableTask implementations</li>
 *   <li><b>S_ROUTE/P_ROUTE:</b> Returns InvokableRoute implementations</li>
 * </ul>
 *
 * @see WorkflowComponantFactory
 * @see InvokableTask
 * @see InvokableRoute
 */
@Service
public class DefaultWorkflowComponentFactory implements WorkflowComponantFactory {

    private final ListableBeanFactory beans;

    /** @param beans where task and route beans are looked up, by the component name of the step */
    public DefaultWorkflowComponentFactory(ListableBeanFactory beans) {
        this.beans = beans;
    }

    @Override
    public Object getObject(WorkflowContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("WorkflowContext cannot be null");
        }

        StepType componentType = ctx.getCompType();
        String componentName = ctx.getCompName();

        if (componentType == null) {
            throw new IllegalArgumentException(
                "Component type cannot be null for component '" + componentName + "'");
        }

        if (componentName == null || componentName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Component name cannot be null or empty for component type " + componentType);
        }

        return switch (componentType) {
            case TASK -> task(componentName, ctx);
            case S_ROUTE, P_ROUTE, P_ROUTE_DYNAMIC -> route(componentName, ctx);
            case PAUSE, P_JOIN, PERSIST -> throw new IllegalArgumentException(
                String.format(
                    "Unsupported component type: %s for component '%s'. " +
                    "Only TASK, S_ROUTE, P_ROUTE, and P_ROUTE_DYNAMIC are supported. " +
                    "Other types should be handled by the Runtime Service.",
                    componentType, componentName
                )
            );
        };
    }

    private InvokableTask task(String beanName, WorkflowContext ctx) {
        return switch (bean(beanName, "task")) {
            case ContextualTask task -> () -> task.execute(ctx);
            case InvokableTask task -> task;
            case Object other -> throw new IllegalStateException(
                    "Bean '" + beanName + "' is neither an InvokableTask nor a ContextualTask");
        };
    }

    private InvokableRoute route(String beanName, WorkflowContext ctx) {
        return switch (bean(beanName, "route")) {
            case ContextualRoute route -> () -> route.execute(ctx);
            case InvokableRoute route -> route;
            case Object other -> throw new IllegalStateException(
                    "Bean '" + beanName + "' is neither an InvokableRoute nor a ContextualRoute");
        };
    }

    private Object bean(String beanName, String kind) {
        if (!beans.containsBean(beanName)) {
            throw new IllegalStateException("No " + kind + " bean found with name: '" + beanName + "'");
        }
        return beans.getBean(beanName);
    }
}
