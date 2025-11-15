package com.anode.workflow.spring.autoconfigure.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariables;

public class FluentWorkflowBuilder {

    private final WorkflowEngine engine;
    private final String caseId;

    private final List<String> taskNames = new ArrayList<>();
    private final Map<String, Object> variables = new HashMap<>();
    private String engineName;

    public FluentWorkflowBuilder(WorkflowEngine engine, String caseId) {
        this.engine = engine;
        this.caseId = caseId;
    }

    /**
     * Select a specific RuntimeService by name (optional).
     */
    public FluentWorkflowBuilder engine(String engineName) {
        this.engineName = engineName;
        return this;
    }

    /**
     * Add a task to the workflow by taskName (from TaskScanner).
     */
    public FluentWorkflowBuilder task(String taskName) {
        taskNames.add(taskName);
        return this;
    }

    /**
     * Add multiple tasks.
     */
    public FluentWorkflowBuilder tasks(List<String> names) {
        taskNames.addAll(names);
        return this;
    }

    /**
     * Add a workflow variable.
     */
    public FluentWorkflowBuilder variable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    /**
     * Add multiple variables.
     */
    public FluentWorkflowBuilder variables(Map<String, Object> vars) {
        variables.putAll(vars);
        return this;
    }

    /**
     * Build the workflow definition without starting.
     */
    public WorkflowDefinition buildDefinition() {
        return engine.buildDefinition(taskNames);
    }

    /**
     * Build WorkflowVariables object without starting.
     */
    public WorkflowVariables buildVariables() {
        WorkflowVariables vars = new WorkflowVariables();
        variables.forEach((k, v) -> vars.setValue(k, com.anode.workflow.entities.workflows.WorkflowVariable.WorkflowVariableType.OBJECT, v));
        return vars;
    }

    /**
     * Start workflow using default RuntimeService.
     */
    public WorkflowContext start() {
        if (engineName != null) {
            return engine.startWorkflow(caseId, engineName, taskNames, variables);
        }
        return engine.startWorkflow(caseId, taskNames, variables);
    }

    /**
     * Start workflow using pre-built WorkflowDefinition and WorkflowVariables.
     */
    public WorkflowContext start(WorkflowDefinition def, WorkflowVariables vars) {
        if (engineName != null) {
            return engine.startWorkflow(caseId, engineName, def, vars);
        }
        return engine.startWorkflow(caseId, def, vars);
    }
}
