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
     * Could be a task or a route depending on StepType.
     */
    @Override
    public Object getObject(WorkflowContext ctx) {
        // TODO : implements other strategy for all component type
        if (ctx.getCompType() == StepType.TASK) {
            return findTask(ctx.getCompName());
        }

        if (ctx.getCompType() == StepType.S_ROUTE) {
            return findRoute(ctx.getCompName());
        }

        throw new IllegalArgumentException(
                "Unsupported component type: " + ctx.getCompType()
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
