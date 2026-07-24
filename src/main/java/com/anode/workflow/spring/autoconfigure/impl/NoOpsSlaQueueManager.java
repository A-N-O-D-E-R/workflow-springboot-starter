package com.anode.workflow.spring.autoconfigure.impl;

import java.util.List;

import com.anode.workflow.entities.sla.Milestone;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.SlaQueueManager;

/**
 * No-operation implementation of SlaQueueManager.
 *
 * <p>Used as a default SLA queue manager when SLA management is not configured.
 * This manager ignores all queue operations without performing any action.
 *
 * @see SlaQueueManager
 */
public class NoOpsSlaQueueManager implements SlaQueueManager {

    /**
     * Dequeues a single SLA milestone (no-op implementation).
     *
     * @param context the workflow context
     * @param milestoneName the name of the milestone to dequeue
     */
    @Override
    public void dequeue(WorkflowContext context, String milestoneName) {
        // No operation
    }

    /**
     * Dequeues all SLA milestones for a workflow (no-op implementation).
     *
     * @param context the workflow context
     */
    @Override
    public void dequeueAll(WorkflowContext context) {
        // No operation
    }

    /**
     * Enqueues SLA milestones for a workflow (no-op implementation).
     *
     * @param context the workflow context
     * @param milestones list of milestones to enqueue
     */
    @Override
    public void enqueue(WorkflowContext context, List<Milestone> milestones) {
        // No operation
    }
}
