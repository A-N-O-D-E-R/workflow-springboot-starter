package com.anode.workflow.spring.autoconfigure.model;

import java.util.List;

import com.anode.workflow.entities.steps.Step.StepType;

public class WorkflowNode {

    private String name;
    private String component;
    private StepType type; // null | p_route | p_join
    private String next;
    private List<Branch> branches;

    public static class Branch {
        public String name;
        public String next;
    }

    public WorkflowNode(String name) { this.name = name; }

    // getters/setters...

    public String getName() { return name; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }

    public StepType getType() { return type; }
    public void setType(StepType type) { this.type = type; }

    public String getNext() { return next; }
    public void setNext(String next) { this.next = next; }

    public List<Branch> getBranches() { return branches; }
    public void setBranches(List<Branch> branches) { this.branches = branches; }
}

