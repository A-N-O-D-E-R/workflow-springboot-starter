package com.anode.workflow.spring.autoconfigure.runtime;

import java.util.function.Consumer;

import com.anode.workflow.spring.autoconfigure.model.WorkflowNode;

/**
 * Builder for configuring workflow steps within a route branch.
 *
 * <p>Allows adding tasks and nested routes within a branch. After configuring
 * all steps in the branch, call {@link #join(String)} to complete the branch
 * and return to the parent builder.
 *
 * <p><b>Example usage:</b>
 * <pre>
 * builder.route("checkPayment", r -> r
 *     .branch("success")
 *         .task("processPayment")
 *         .task("updateInventory")
 *         .join("allBranchesDone"))
 * </pre>
 *
 * @see FluentWorkflowBuilder
 * @see RouteBuilder
 */
public class BranchBuilder {

    private final FluentWorkflowBuilder parent;
    private final RouteBuilder routeBuilder;
    private final WorkflowNode.Branch branch;
    private WorkflowNode last;

    /**
     * Constructs a BranchBuilder for the given branch.
     *
     * @param parent the parent fluent workflow builder
     * @param rb the route builder that owns this branch
     * @param b the branch to configure
     */
    public BranchBuilder(FluentWorkflowBuilder parent, RouteBuilder rb, WorkflowNode.Branch b) {
        this.parent = parent;
        this.routeBuilder = rb;
        this.branch = b;
    }

    /**
     * Add a task step to this branch.
     *
     * @param component the bean name of the task component
     * @return this builder for chaining
     */
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

    /**
     * Add a nested route step to this branch.
     *
     * @param component the bean name of the route component
     * @param consumer callback to configure the route branches
     * @return this builder for chaining
     */
    public BranchBuilder route(String component, Consumer<RouteBuilder> consumer) {
        parent.route(component, consumer);
        last = parent.getNodes().get(parent.getNodes().size() - 1);
        return this;
    }

    /**
     * Complete this branch and add a join step to synchronize all branches.
     *
     * @param name the name of the join step
     * @return the parent fluent workflow builder for chaining
     */
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

