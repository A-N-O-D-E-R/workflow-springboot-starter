package com.anode.workflow.spring.autoconfigure.runtime;

import java.util.*;
import java.util.function.Consumer;

import com.anode.workflow.entities.steps.Step.StepType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.spring.autoconfigure.model.WorkflowNode;

public class FluentWorkflowBuilder {

    private final WorkflowEngine engine;
    private final String caseId;
    private final Map<String, Object> variables = new HashMap<>();
    private final List<WorkflowNode> nodes = new ArrayList<>();

    private WorkflowNode current;
    private String engineName;

    public FluentWorkflowBuilder(WorkflowEngine engine, String caseId) {
        if (engine == null) throw new IllegalArgumentException("WorkflowEngine cannot be null");
        if (caseId == null || caseId.isEmpty()) throw new IllegalArgumentException("Case ID cannot be empty");

        this.engine = engine;
        this.caseId = caseId;
    }

    /** Select engine */
    public FluentWorkflowBuilder engine(String engineName) {
        this.engineName = engineName;
        return this;
    }

    // ---------------------------------------------------------------------
    // TASK
    // ---------------------------------------------------------------------
    public FluentWorkflowBuilder task(String component) {
        WorkflowNode node = new WorkflowNode(current==null?WorkflowEngine.START_STEP:component);
        node.setComponent(component);
        node.setNext(WorkflowEngine.END_STEP);
        link(node);
        nodes.add(node);
        current = node;
        return this;
    }

    // ---------------------------------------------------------------------
    // ROUTE
    // ---------------------------------------------------------------------
    public FluentWorkflowBuilder route(String component, Consumer<RouteBuilder> consumer) {
        WorkflowNode route = new WorkflowNode(component);
        route.setComponent(component);
        route.setType(StepType.S_ROUTE);
        route.setBranches(new ArrayList<>());

        link(route);
        nodes.add(route);

        RouteBuilder rb = new RouteBuilder(this, route);
        consumer.accept(rb);

        // Continue after last join of route
        current = rb.getAfterNode();
        return this;
    }

    public FluentWorkflowBuilder parrallelRoute(String component, Consumer<RouteBuilder> consumer) {
        WorkflowNode route = new WorkflowNode(component);
        route.setComponent(component);
        route.setType(StepType.P_ROUTE);
        route.setBranches(new ArrayList<>());

        link(route);
        nodes.add(route);

        RouteBuilder rb = new RouteBuilder(this, route);
        consumer.accept(rb);

        // Continue after last join of route
        current = rb.getAfterNode();
        return this;
    }

    // ---------------------------------------------------------------------
    // JOIN
    // ---------------------------------------------------------------------
    public FluentWorkflowBuilder join(String name) {
        WorkflowNode join = new WorkflowNode(name);
        join.setType(StepType.P_JOIN);
        join.setNext(WorkflowEngine.END_STEP);

        link(join);
        nodes.add(join);
        current = join;
        return this;
    }

    // ---------------------------------------------------------------------
    // Variables
    // ---------------------------------------------------------------------

    public FluentWorkflowBuilder variable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    public FluentWorkflowBuilder variables(Map<String, Object> vars) {
        variables.putAll(vars);
        return this;
    }

    // ---------------------------------------------------------------------
    // Build Definition
    // ---------------------------------------------------------------------
    public WorkflowDefinition buildDefinition() {
        return engine.buildDefinitionFromNodes(nodes);
    }

    public WorkflowVariables buildVariables() {
        WorkflowVariables vars = new WorkflowVariables();
        variables.forEach((k, v) ->
                vars.setValue(k,
                        com.anode.workflow.entities.workflows.WorkflowVariable.WorkflowVariableType.OBJECT,
                        v));
        return vars;
    }

    // ---------------------------------------------------------------------
    // Start workflow
    // ---------------------------------------------------------------------

    public WorkflowContext start() {
        WorkflowDefinition def = buildDefinition();
        WorkflowVariables vars = buildVariables();

        if (engineName != null)
            return engine.startWorkflow(caseId, engineName, def, vars);

        return engine.startWorkflow(caseId, def, vars);
    }

    public WorkflowContext start(WorkflowDefinition def, WorkflowVariables vars) {
        if (engineName != null)
            return engine.startWorkflow(caseId, engineName, def, vars);

        return engine.startWorkflow(caseId, def, vars);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    private void link(WorkflowNode newNode) {
        if (current != null) current.setNext(newNode.getName());
    }

    List<WorkflowNode> getNodes() {
        return nodes;
    }
}
