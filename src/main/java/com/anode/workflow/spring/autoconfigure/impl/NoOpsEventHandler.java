package com.anode.workflow.spring.autoconfigure.impl;

import com.anode.workflow.entities.events.EventType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.EventHandler;

public class NoOpsEventHandler implements EventHandler{

    @Override
    public void invoke(EventType arg0, WorkflowContext arg1) {
    }
    
}
