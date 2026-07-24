package com.anode.workflow.spring.autoconfigure.impl;

import com.anode.workflow.entities.events.EventType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.EventHandler;

/**
 * No-operation implementation of EventHandler.
 *
 * <p>Used as a default event handler when no custom event handlers are configured.
 * This handler ignores all workflow events without performing any action.
 *
 * @see EventHandler
 */
public class NoOpsEventHandler implements EventHandler {

    /**
     * Invokes the event handler (no-op implementation).
     *
     * @param eventType the type of event
     * @param context the workflow context
     */
    @Override
    public void invoke(EventType eventType, WorkflowContext context) {
        // No operation
    }
}
