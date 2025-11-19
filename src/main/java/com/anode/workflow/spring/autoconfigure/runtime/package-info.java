/**
 * Runtime components for workflow execution and management.
 *
 * <p>This package provides the runtime facade and fluent API for executing
 * workflows in Spring Boot applications.
 *
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.runtime.WorkflowEngine} -
 *       Main facade for workflow execution with validation and error handling</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilder} -
 *       Fluent API for building and executing workflows</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilderFactory} -
 *       Factory for creating workflow builders</li>
 *   <li>{@link com.anode.workflow.spring.autoconfigure.runtime.WorkflowService} -
 *       Service for managing multiple workflow engines</li>
 * </ul>
 *
 * <h2>Workflow Execution</h2>
 *
 * <h3>Simple Execution</h3>
 * <pre>{@code
 * @Autowired
 * private WorkflowEngine engine;
 *
 * // Start workflow with task names
 * WorkflowContext ctx = engine.startWorkflow(
 *     "case-123",
 *     Arrays.asList("validateOrder", "processPayment", "shipOrder"),
 *     Map.of("orderId", 456, "customerId", 789)
 * );
 * }</pre>
 *
 * <h3>Fluent API</h3>
 * <pre>{@code
 * @Autowired
 * private FluentWorkflowBuilderFactory builderFactory;
 *
 * // Build and execute workflow using fluent API
 * WorkflowContext ctx = builderFactory.create("case-456")
 *     .task("validateOrder")
 *     .task("processPayment")
 *     .task("shipOrder")
 *     .variable("orderId", 456)
 *     .variable("customerId", 789)
 *     .start();
 * }</pre>
 *
 * <h3>Multiple Engines</h3>
 * <pre>{@code
 * // Use specific engine by name
 * WorkflowContext ctx = engine.startWorkflow(
 *     "case-789",
 *     "secondary-engine",  // engine name
 *     Arrays.asList("task1", "task2"),
 *     Map.of("key", "value")
 * );
 * }</pre>
 *
 * <h2>Input Validation</h2>
 * <p>The runtime components perform comprehensive input validation:
 * <ul>
 *   <li>Case ID must not be null or empty</li>
 *   <li>Task names must not be null or empty</li>
 *   <li>Task names must be unique in workflow</li>
 *   <li>All tasks must be registered in the task scanner</li>
 *   <li>Variable keys must not be null or empty</li>
 * </ul>
 *
 * <p>Validation errors throw {@code IllegalArgumentException} with clear,
 * actionable error messages including index positions for list errors.
 *
 * <h2>Error Handling</h2>
 * <p>Common exceptions:
 * <ul>
 *   <li>{@code IllegalArgumentException} - Invalid input (null, empty, duplicates)</li>
 *   <li>{@code IllegalStateException} - No engine configured or engine not found</li>
 * </ul>
 *
 * @see com.anode.workflow.service.runtime.RuntimeService
 * @see com.anode.workflow.entities.workflows.WorkflowContext
 * @see com.anode.workflow.entities.workflows.WorkflowDefinition
 * @since 0.0.1
 */
package com.anode.workflow.spring.autoconfigure.runtime;
