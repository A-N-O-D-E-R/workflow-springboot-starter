package com.anode.workflow.spring.autoconfigure.impl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.anode.workflow.entities.steps.InvokableRoute;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.Step.StepType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.WorkflowComponantFactory;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DefaultWorkflowComponentFactory implements WorkflowComponantFactory {

    private final ApplicationContext context;

    private final ObjectProvider<InvokableTask> taskProvider;

    private final ObjectProvider<InvokableRoute> routeProvider;

    /** Caches all task and route beans by name for fast lookup */
    private Map<String, InvokableTask> taskBeans;
    private Map<String, InvokableRoute> routeBeans;

    public DefaultWorkflowComponentFactory(
            ApplicationContext context,
            ObjectProvider<InvokableTask> taskProvider,
            ObjectProvider<InvokableRoute> routeProvider
    ) {
        this.context = context;
        this.taskProvider = taskProvider;
        this.routeProvider = routeProvider;
    }

    @PostConstruct
    public void init() {
        // lazy: beans are not created until iterated or accessed
        taskBeans = taskProvider.stream().collect(Collectors.toUnmodifiableMap(
                bean -> bean.getClass().getName(),
                bean -> bean
        ));

        routeBeans = routeProvider.stream().collect(Collectors.toUnmodifiableMap(
                bean -> bean.getClass().getName(),
                bean -> bean
        ));
    }


    /**
     * Returns the workflow component based on context.
     *
     * <p>Supported component types:
     * <ul>
     *   <li>{@link StepType#TASK} - Returns an {@link InvokableTask} implementation</li>
     *   <li>{@link StepType#S_ROUTE} - Returns an {@link InvokableRoute} implementation (sequential route)</li>
     *   <li>{@link StepType#P_ROUTE} - Returns an {@link InvokableRoute} implementation (parallel route)</li>
     *   <li>{@link StepType#P_ROUTE_DYNAMIC} - Returns an {@link InvokableRoute} implementation (dynamic parallel route)</li>
     * </ul>
     *
     * <p>Unsupported types (handled by Runtime Service):
     * <ul>
     *   <li>{@link StepType#PAUSE} - Workflow pause point</li>
     *   <li>{@link StepType#P_JOIN} - Parallel join synchronization</li>
     *   <li>{@link StepType#PERSIST} - Workflow persistence point</li>
     * </ul>
     *
     * @param ctx the workflow context containing component type and name
     * @return the workflow component (InvokableTask or InvokableRoute)
     * @throws IllegalArgumentException if the component type is not supported or context/type/name is null/empty
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

        return switch (componentType) {
            case TASK -> findTask(componentName);
            case S_ROUTE, P_ROUTE, P_ROUTE_DYNAMIC -> findRoute(componentName);
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
