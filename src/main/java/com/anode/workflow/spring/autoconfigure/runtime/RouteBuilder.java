package com.anode.workflow.spring.autoconfigure.runtime;

import com.anode.workflow.spring.autoconfigure.model.WorkflowNode;

public class RouteBuilder {

    private final FluentWorkflowBuilder parent;
    private final WorkflowNode routeNode;
    private WorkflowNode afterNode;

    public RouteBuilder(FluentWorkflowBuilder parent, WorkflowNode node) {
        this.parent = parent;
        this.routeNode = node;
    }

    public BranchBuilder branch(String name) {
        WorkflowNode.Branch b = new WorkflowNode.Branch();
        b.name = name;
        b.next = null;

        routeNode.getBranches().add(b);
        return new BranchBuilder(parent, this, b);
    }

    public void setAfterNode(WorkflowNode node) {
        this.afterNode = node;
    }

    public WorkflowNode getAfterNode() {
        return this.afterNode;
    }
}

