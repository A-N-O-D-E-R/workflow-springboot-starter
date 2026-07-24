package com.anode.workflow.spring.autoconfigure.runtime;

import com.anode.workflow.spring.autoconfigure.model.WorkflowNode;

/**
 * Builder for configuring route branches within a workflow.
 *
 * <p>Used to add branches to a route step. Each branch represents a conditional path
 * in the workflow that can be taken based on routing logic.
 *
 * <p><b>Example usage:</b>
 * <pre>
 * builder.route("paymentRoute", r -> r
 *     .branch("success")
 *         .next("confirmOrder")
 *     .branch("failed")
 *         .next("notifyFailure"))
 * </pre>
 *
 * @see FluentWorkflowBuilder
 * @see BranchBuilder
 */
public class RouteBuilder {

    private final FluentWorkflowBuilder parent;
    private final WorkflowNode routeNode;
    private WorkflowNode afterNode;

    /**
     * Constructs a RouteBuilder for the given route node.
     *
     * @param parent the parent fluent workflow builder
     * @param node the route workflow node to configure
     */
    public RouteBuilder(FluentWorkflowBuilder parent, WorkflowNode node) {
        this.parent = parent;
        this.routeNode = node;
    }

    /**
     * Create and add a branch to this route.
     *
     * @param name the name/identifier of the branch
     * @return a BranchBuilder to configure the branch
     */
    public BranchBuilder branch(String name) {
        WorkflowNode.Branch b = new WorkflowNode.Branch();
        b.name = name;
        b.next = null;

        routeNode.getBranches().add(b);
        return new BranchBuilder(parent, this, b);
    }

    /**
     * Set the node to execute after this route completes.
     *
     * @param node the next workflow node
     */
    public void setAfterNode(WorkflowNode node) {
        this.afterNode = node;
    }

    /**
     * Get the node to execute after this route completes.
     *
     * @return the next workflow node
     */
    public WorkflowNode getAfterNode() {
        return this.afterNode;
    }
}

