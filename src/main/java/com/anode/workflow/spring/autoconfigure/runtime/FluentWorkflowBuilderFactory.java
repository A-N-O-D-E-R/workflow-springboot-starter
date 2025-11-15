package com.anode.workflow.spring.autoconfigure.runtime;

import org.springframework.stereotype.Component;

/**
 * Factory for FluentWorkflowBuilder.
 */
@Component
public class FluentWorkflowBuilderFactory {

    private final WorkflowEngine engine;

    public FluentWorkflowBuilderFactory(WorkflowEngine engine) {
        this.engine = engine;
    }

    /**
     * Create a new builder for a given caseId.
     */
    public FluentWorkflowBuilder builder(String caseId) {
        return new FluentWorkflowBuilder(engine, caseId);
    }
}
