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
        if (engine == null) {
            throw new IllegalArgumentException("WorkflowEngine cannot be null");
        }
        if (caseId == null || caseId.trim().isEmpty()) {
            throw new IllegalArgumentException("Case ID cannot be null or empty");
        }
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
     *
     * @param taskName the task name to add (cannot be null or empty)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if taskName is null or empty
     */
    public FluentWorkflowBuilder task(String taskName) {
        if (taskName == null || taskName.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be null or empty");
        }
        taskNames.add(taskName);
        return this;
    }

    /**
     * Add multiple tasks.
     *
     * @param names list of task names to add (cannot be null)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if names is null
     */
    public FluentWorkflowBuilder tasks(List<String> names) {
        if (names == null) {
            throw new IllegalArgumentException("Task names list cannot be null");
        }
        // Validate each task name in the list
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Task names list cannot contain null or empty entries");
            }
        }
        taskNames.addAll(names);
        return this;
    }

    /**
     * Add a workflow variable.
     *
     * @param key the variable key (cannot be null or empty)
     * @param value the variable value
     * @return this builder for method chaining
     * @throws IllegalArgumentException if key is null or empty
     */
    public FluentWorkflowBuilder variable(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Variable key cannot be null or empty");
        }
        variables.put(key, value);
        return this;
    }

    /**
     * Add multiple variables.
     *
     * @param vars map of variables to add (cannot be null)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if vars is null or contains null/empty keys
     */
    public FluentWorkflowBuilder variables(Map<String, Object> vars) {
        if (vars == null) {
            throw new IllegalArgumentException("Variables map cannot be null");
        }
        // Validate all keys
        for (String key : vars.keySet()) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("Variables map cannot contain null or empty keys");
            }
        }
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
