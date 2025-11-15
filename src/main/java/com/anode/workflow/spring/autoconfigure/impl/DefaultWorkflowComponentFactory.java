package com.anode.workflow.spring.autoconfigure.impl;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.anode.workflow.entities.steps.InvokableRoute;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.Step.StepType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.WorkflowComponantFactory;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Service
public class DefaultWorkflowComponentFactory implements WorkflowComponantFactory {

    private final ApplicationContext context;

    /** Caches all task and route beans by name for fast lookup */
    private Map<String, InvokableTask> taskBeans;
    private Map<String, InvokableRoute> routeBeans;

    public DefaultWorkflowComponentFactory(ApplicationContext context) {
        this.context = context;
    }

    @PostConstruct
    public void init() {
        // cache all beans implementing InvokableTask / InvokableRoute
        taskBeans = context.getBeansOfType(InvokableTask.class);
        routeBeans = context.getBeansOfType(InvokableRoute.class);
    }

    /**
     * Returns the workflow component based on context.
     *
     * <p>Supported component types:
     * <ul>
     *   <li>{@link StepType#TASK} - Returns an {@link InvokableTask} implementation</li>
     *   <li>{@link StepType#S_ROUTE} - Returns an {@link InvokableRoute} implementation</li>
     * </ul>
     *
     * @param ctx the workflow context containing component type and name
     * @return the workflow component (InvokableTask or InvokableRoute)
     * @throws IllegalArgumentException if the component type is not supported
     * @throws IllegalStateException if the component bean is not found
     */
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

        if (componentType == StepType.TASK) {
            return findTask(componentName);
        }

        if (componentType == StepType.S_ROUTE) {
            return findRoute(componentName);
        }

        // Only TASK and S_ROUTE are currently supported
        // Other types like P_ROUTE, SUB_PROCESS, etc. are not implemented
        throw new IllegalArgumentException(
                String.format("Unsupported component type: %s for component '%s'. " +
                             "Only TASK and S_ROUTE are currently supported. " +
                             "If you need support for other component types, please file an issue.",
                             componentType, componentName)
        );
    }

    /**
     * Lookup a task bean by name and ensure it implements InvokableTask.
     */
    public InvokableTask findTask(String beanName) {
        InvokableTask task = taskBeans.get(beanName);
        if (task == null) {
            throw new IllegalStateException(
                    "No InvokableTask bean found with name: '" + beanName + "'"
            );
        }
        return task;
    }

    /**
     * Lookup a route bean by name and ensure it implements InvokableRoute.
     */
    public InvokableRoute findRoute(String beanName) {
        InvokableRoute route = routeBeans.get(beanName);
        if (route == null) {
            throw new IllegalStateException(
                    "No InvokableRoute bean found with name: '" + beanName + "'"
            );
        }
        return route;
    }
}
