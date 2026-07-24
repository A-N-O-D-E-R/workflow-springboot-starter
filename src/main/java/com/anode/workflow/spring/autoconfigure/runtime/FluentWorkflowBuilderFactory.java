package com.anode.workflow.spring.autoconfigure.runtime;

import org.springframework.stereotype.Component;

/**
 * Factory for creating {@link FluentWorkflowBuilder} instances.
 *
 * <p>This factory is registered as a Spring component and can be injected
 * to create new workflow builders. It simplifies the workflow builder creation
 * by managing the engine dependency.
 *
 * <p><b>Example usage:</b>
 * <pre>
 * &#64;Autowired
 * private FluentWorkflowBuilderFactory builderFactory;
 *
 * public void startWorkflow() {
 *     FluentWorkflowBuilder builder = builderFactory.builder("case-123");
 *     WorkflowContext ctx = builder
 *         .task("taskA")
 *         .task("taskB")
 *         .start();
 * }
 * </pre>
 *
 * @see FluentWorkflowBuilder
 * @see WorkflowEngine
 */
@Component
public class FluentWorkflowBuilderFactory {

    private final WorkflowEngine engine;

    /**
     * Constructs the factory with the given workflow engine.
     *
     * @param engine the workflow engine
     */
    public FluentWorkflowBuilderFactory(WorkflowEngine engine) {
        this.engine = engine;
    }

    /**
     * Creates a new fluent workflow builder for the given case ID.
     *
     * @param caseId the unique case identifier
     * @return a new FluentWorkflowBuilder instance
     */
    public FluentWorkflowBuilder builder(String caseId) {
        return new FluentWorkflowBuilder(engine, caseId);
    }
}
