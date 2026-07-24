package com.anode.workflow.spring.autoconfigure.runtime;

import java.util.*;
import java.util.function.Consumer;

import com.anode.workflow.entities.steps.Step.StepType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.spring.autoconfigure.model.WorkflowNode;

/**
 * Fluent builder for constructing workflow definitions programmatically.
 *
 * <p>Provides a fluent API to build complex workflows with tasks, sequential/parallel routes,
 * joins, and workflow variables. This builder makes it easy to define workflows without
 * manually creating WorkflowDefinition objects.
 *
 * <p><b>Example usage:</b>
 * <pre>
 * FluentWorkflowBuilder builder = engine.builder("case-123");
 * WorkflowContext ctx = builder
 *     .task("processPayment")
 *     .route("paymentRoute", r -> r
 *         .branch("success", "confirmOrder")
 *         .branch("failed", "notifyFailure"))
 *     .task("confirmOrder")
 *     .variable("userId", 42)
 *     .variable("amount", 99.99)
 *     .start();
 * </pre>
 *
 * @see WorkflowEngine
 * @see RouteBuilder
 */
public class FluentWorkflowBuilder {

    private final WorkflowEngine engine;
    private final String caseId;
    private final Map<String, Object> variables = new HashMap<>();
    private final List<WorkflowNode> nodes = new ArrayList<>();

    private WorkflowNode current;
    private String engineName;

    /**
     * Constructs a FluentWorkflowBuilder with the given engine and case ID.
     *
     * @param engine the workflow engine (cannot be null)
     * @param caseId the unique case identifier (cannot be empty)
     * @throws IllegalArgumentException if engine is null or caseId is empty
     */
    public FluentWorkflowBuilder(WorkflowEngine engine, String caseId) {
        if (engine == null) throw new IllegalArgumentException("WorkflowEngine cannot be null");
        if (caseId == null || caseId.isEmpty()) throw new IllegalArgumentException("Case ID cannot be empty");

        this.engine = engine;
        this.caseId = caseId;
    }

    /**
     * Select a specific runtime engine for this workflow.
     * If not specified, the default engine is used.
     *
     * @param engineName the name of the runtime engine
     * @return this builder for chaining
     */
    public FluentWorkflowBuilder engine(String engineName) {
        this.engineName = engineName;
        return this;
    }

    /**
     * Add a task step to the workflow.
     *
     * @param component the bean name of the task component
     * @return this builder for chaining
     */
    public FluentWorkflowBuilder task(String component) {
        WorkflowNode node = new WorkflowNode(current==null?WorkflowEngine.START_STEP:component);
        node.setComponent(component);
        node.setNext(WorkflowEngine.END_STEP);
        link(node);
        nodes.add(node);
        current = node;
        return this;
    }

    /**
     * Add a sequential route step to the workflow.
     *
     * <p>Routes allow conditional branching where one branch is taken based on
     * routing logic. Sequential routes execute branches in order.
     *
     * @param component the bean name of the route component
     * @param consumer callback to configure route branches
     * @return this builder for chaining
     *
     * @see RouteBuilder
     */
    public FluentWorkflowBuilder route(String component, Consumer<RouteBuilder> consumer) {
        WorkflowNode route = new WorkflowNode(component);
        route.setComponent(component);
        route.setType(StepType.S_ROUTE);
        route.setBranches(new ArrayList<>());

        link(route);
        nodes.add(route);

        RouteBuilder rb = new RouteBuilder(this, route);
        consumer.accept(rb);

        current = rb.getAfterNode();
        return this;
    }

    /**
     * Add a parallel route step to the workflow.
     *
     * <p>Parallel routes execute multiple branches concurrently and require a join
     * step to synchronize before continuing.
     *
     * @param component the bean name of the route component
     * @param consumer callback to configure route branches
     * @return this builder for chaining
     *
     * @see RouteBuilder
     */
    public FluentWorkflowBuilder parrallelRoute(String component, Consumer<RouteBuilder> consumer) {
        WorkflowNode route = new WorkflowNode(component);
        route.setComponent(component);
        route.setType(StepType.P_ROUTE);
        route.setBranches(new ArrayList<>());

        link(route);
        nodes.add(route);

        RouteBuilder rb = new RouteBuilder(this, route);
        consumer.accept(rb);

        current = rb.getAfterNode();
        return this;
    }

    /**
     * Add a join step to synchronize parallel branches.
     *
     * @param name the name of the join step
     * @return this builder for chaining
     */
    public FluentWorkflowBuilder join(String name) {
        WorkflowNode join = new WorkflowNode(name);
        join.setType(StepType.P_JOIN);
        join.setNext(WorkflowEngine.END_STEP);

        link(join);
        nodes.add(join);
        current = join;
        return this;
    }

    /**
     * Add a single workflow variable.
     *
     * @param key the variable name
     * @param value the variable value
     * @return this builder for chaining
     */
    public FluentWorkflowBuilder variable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    /**
     * Add multiple workflow variables at once.
     *
     * @param vars map of variable names to values
     * @return this builder for chaining
     */
    public FluentWorkflowBuilder variables(Map<String, Object> vars) {
        variables.putAll(vars);
        return this;
    }

    /**
     * Build the workflow definition from the configured nodes.
     *
     * @return the workflow definition
     */
    public WorkflowDefinition buildDefinition() {
        return engine.buildDefinitionFromNodes(nodes);
    }

    /**
     * Build the workflow variables from the configured variables.
     *
     * @return the workflow variables
     */
    public WorkflowVariables buildVariables() {
        WorkflowVariables vars = new WorkflowVariables();
        variables.forEach((k, v) ->
                vars.setValue(k,
                        com.anode.workflow.entities.workflows.WorkflowVariable.WorkflowVariableType.OBJECT,
                        v));
        return vars;
    }

    /**
     * Start the workflow with the configured definition and variables.
     *
     * @return the workflow context representing the running workflow
     */
    public WorkflowContext start() {
        WorkflowDefinition def = buildDefinition();
        WorkflowVariables vars = buildVariables();

        if (engineName != null)
            return engine.startWorkflow(caseId, engineName, def, vars);

        return engine.startWorkflow(caseId, def, vars);
    }

    /**
     * Start the workflow with custom definition and variables.
     *
     * @param def the workflow definition
     * @param vars the workflow variables
     * @return the workflow context representing the running workflow
     */
    public WorkflowContext start(WorkflowDefinition def, WorkflowVariables vars) {
        if (engineName != null)
            return engine.startWorkflow(caseId, engineName, def, vars);

        return engine.startWorkflow(caseId, def, vars);
    }

    private void link(WorkflowNode newNode) {
        if (current != null) current.setNext(newNode.getName());
    }

    List<WorkflowNode> getNodes() {
        return nodes;
    }
}
