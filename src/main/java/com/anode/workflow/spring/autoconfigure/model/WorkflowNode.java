package com.anode.workflow.spring.autoconfigure.model;

import java.util.List;

import com.anode.workflow.entities.steps.Step.StepType;

/**
 * Represents a node in a workflow definition used by the fluent builder API.
 *
 * <p>A workflow node can represent:
 * <ul>
 *   <li><b>Task:</b> A single step that executes a task component (type is null)</li>
 *   <li><b>Sequential Route:</b> A conditional branch point (type is S_ROUTE)</li>
 *   <li><b>Parallel Route:</b> A parallel branch point (type is P_ROUTE)</li>
 *   <li><b>Join:</b> A synchronization point for parallel branches (type is P_JOIN)</li>
 * </ul>
 *
 * <p>This model is used internally by the fluent workflow builder to construct
 * workflow definitions programmatically before converting them to runtime definitions.
 *
 * @see com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilder
 */
public class WorkflowNode {

    private String name;
    private String component;
    private StepType type;
    private String next;
    private List<Branch> branches;

    /**
     * Represents a branch within a route node.
     */
    public static class Branch {
        /** The name/identifier of this branch */
        public String name;
        /** The next node to execute if this branch is taken */
        public String next;
    }

    /**
     * Constructs a WorkflowNode with the given name.
     *
     * @param name the unique name of this node in the workflow
     */
    public WorkflowNode(String name) { this.name = name; }

    /**
     * Gets the unique name of this node.
     *
     * @return the node name
     */
    public String getName() { return name; }

    /**
     * Gets the component bean name (task or route).
     *
     * @return the component bean name
     */
    public String getComponent() { return component; }

    /**
     * Sets the component bean name (task or route).
     *
     * @param component the component bean name
     */
    public void setComponent(String component) { this.component = component; }

    /**
     * Gets the type of this node (null for task, or route/join type).
     *
     * @return the step type
     */
    public StepType getType() { return type; }

    /**
     * Sets the type of this node.
     *
     * @param type the step type
     */
    public void setType(StepType type) { this.type = type; }

    /**
     * Gets the name of the next node to execute after this one.
     *
     * @return the next node name
     */
    public String getNext() { return next; }

    /**
     * Sets the name of the next node to execute after this one.
     *
     * @param next the next node name
     */
    public void setNext(String next) { this.next = next; }

    /**
     * Gets the list of branches for route nodes.
     *
     * @return the branches, or null if not a route
     */
    public List<Branch> getBranches() { return branches; }

    /**
     * Sets the list of branches for route nodes.
     *
     * @param branches the branches
     */
    public void setBranches(List<Branch> branches) { this.branches = branches; }
}

