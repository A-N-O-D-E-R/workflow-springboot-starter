package com.anode.workflow.example;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.example.task.ProcessPaymentTask;
import com.anode.workflow.example.task.ValidateOrderTask;
import com.anode.workflow.service.WorkflowComponantFactory;
import com.anode.workflow.spring.autoconfigure.annotations.WorkflowComponentFactory;

@WorkflowComponentFactory
public class ComponentFactory implements WorkflowComponantFactory {

    @Override
    public Object getObject(WorkflowContext ctx) {
        Object order = ctx.getProcessVariables().getObject("order");
        if("processPaymentTask".equals(ctx.getCompName())){
            return new ProcessPaymentTask(order);
        }
        if("validateOrderTask".equals(ctx.getCompName())){
          return new ValidateOrderTask(order);
        }
        return null;
    }
    
}

