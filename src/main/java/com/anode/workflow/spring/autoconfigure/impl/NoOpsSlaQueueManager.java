package com.anode.workflow.spring.autoconfigure.impl;

import java.util.List;

import com.anode.workflow.entities.sla.Milestone;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.SlaQueueManager;

public class NoOpsSlaQueueManager implements SlaQueueManager {

    @Override
    public void dequeue(WorkflowContext arg0, String arg1) {
    }

    @Override
    public void dequeueAll(WorkflowContext arg0) {
    }

    @Override
    public void enqueue(WorkflowContext arg0, List<Milestone> arg1) {
    }
    
}
