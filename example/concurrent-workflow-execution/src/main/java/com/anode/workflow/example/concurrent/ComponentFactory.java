package com.anode.workflow.example.concurrent;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.example.concurrent.task.*;
import com.anode.workflow.service.WorkflowComponantFactory;
import com.anode.workflow.spring.autoconfigure.annotations.WorkflowComponentFactory;

/**
 * Custom component factory for the concurrent workflow execution example.
 *
 * This factory is responsible for instantiating workflow tasks
 * with the necessary dependencies injected from the workflow context.
 */
@WorkflowComponentFactory
public class ComponentFactory implements WorkflowComponantFactory {

    @Override
    public Object getObject(WorkflowContext ctx) {
        // Retrieve the request from workflow variables
        Object request = ctx.getProcessVariables().getObject("request");

        String componentName = ctx.getCompName();

        // Route to appropriate task based on component name
        return switch (componentName) {
            case "validaterequesttask" -> new ValidateRequestTask(request);
            case "processdatatask" -> new ProcessDataTask(request);
            case "generateresulttask" -> new GenerateResultTask(request);
            case "notifyusertask" -> new NotifyUserTask(request);
            default -> null;
        };
    }
}
