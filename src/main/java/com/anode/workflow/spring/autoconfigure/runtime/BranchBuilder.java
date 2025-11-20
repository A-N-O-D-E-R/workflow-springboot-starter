package com.anode.workflow.spring.autoconfigure.runtime;

import java.util.function.Consumer;

import com.anode.workflow.spring.autoconfigure.model.WorkflowNode;

public class BranchBuilder {

    private final FluentWorkflowBuilder parent;
    private final RouteBuilder routeBuilder;
    private final WorkflowNode.Branch branch;
    private WorkflowNode last;

    public BranchBuilder(FluentWorkflowBuilder parent, RouteBuilder rb, WorkflowNode.Branch b) {
        this.parent = parent;
        this.routeBuilder = rb;
        this.branch = b;
    }

    public BranchBuilder task(String component) {
        WorkflowNode node = new WorkflowNode(component);
        node.setComponent(component);
        node.setNext("end");

        if (last != null) last.setNext(node.getName());
        else branch.next = node.getName();

        parent.getNodes().add(node);
        last = node;
        return this;
    }

    public BranchBuilder route(String component, Consumer<RouteBuilder> consumer) {
        parent.route(component, consumer);
        last = parent.getNodes().get(parent.getNodes().size() - 1);
        return this;
    }

    public FluentWorkflowBuilder join(String name) {
        WorkflowNode join = new WorkflowNode(name);
        join.setType(com.anode.workflow.entities.steps.Step.StepType.P_JOIN);
        join.setNext("end");

        if (last != null) last.setNext(join.getName());
        else branch.next = join.getName();

        parent.getNodes().add(join);

        routeBuilder.setAfterNode(join);
        return parent;
    }
}

