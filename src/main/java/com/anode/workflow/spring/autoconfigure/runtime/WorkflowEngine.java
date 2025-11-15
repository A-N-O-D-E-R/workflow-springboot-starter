package com.anode.workflow.spring.autoconfigure.runtime;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.entities.workflows.WorkflowVariable.WorkflowVariableType;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.scanner.TaskScanner;
import com.anode.workflow.spring.autoconfigure.scanner.TaskScanner.TaskDescriptor;

public class WorkflowEngine {

    private static final Logger log =
            LoggerFactory.getLogger(WorkflowEngine.class);

    private final Map<String, RuntimeService> runtimeServices;
    private final TaskScanner taskScanner;

    public WorkflowEngine(Map<String, RuntimeService> services, TaskScanner taskScanner) {
        this.runtimeServices = services;
        this.taskScanner = taskScanner;
    }

    // ------------------------------------------------------------
    // PUBLIC API
    // ------------------------------------------------------------

    /**
     * Start a workflow given a sequence of task names.
     */
    public WorkflowContext startWorkflow(String caseId, List<String> taskNames, Map<String, Object> variableMap) {
        WorkflowDefinition def = buildDefinition(taskNames);
        WorkflowVariables vars = convertVariables(variableMap);
        Entry<String, RuntimeService> rts = getDefaultRuntimeService();
        log.info("WorkflowEngine initialized using {}", rts.getKey());
        return rts.getValue().startCase(caseId, def, vars, null);
    }

    /**
     * Start a workflow given a workflow definition.
     */
    public WorkflowContext startWorkflow(String caseId, WorkflowDefinition definition, Map<String, Object> variableMap) {
        WorkflowVariables vars = convertVariables(variableMap);
        Entry<String, RuntimeService> rts = getDefaultRuntimeService();
        log.info("WorkflowEngine initialized using {}", rts.getKey());
        return rts.getValue().startCase(caseId, definition, vars, null);
    }

    public WorkflowContext startWorkflow(String caseId,  List<String> taskNames, WorkflowVariables variables) {
        WorkflowDefinition def = buildDefinition(taskNames);
        Entry<String, RuntimeService> rts = getDefaultRuntimeService();
        log.info("WorkflowEngine initialized using {}", rts.getKey());
        return rts.getValue().startCase(caseId, def, variables, null);
    }

    public WorkflowContext startWorkflow(String caseId, WorkflowDefinition definition, WorkflowVariables variables) {
        Entry<String, RuntimeService> rts = getDefaultRuntimeService();
        log.info("WorkflowEngine initialized using {}", rts.getKey());
        return rts.getValue().startCase(caseId, definition, variables, null);
    }


    public WorkflowContext startWorkflow(String caseId, String engineName, List<String> taskNames, Map<String, Object> variableMap) {
        WorkflowDefinition def = buildDefinition(taskNames);
        WorkflowVariables vars = convertVariables(variableMap);
        RuntimeService rts = getRuntimeService(engineName);
        log.info("WorkflowEngine initialized using {}", engineName);
        return rts.startCase(caseId, def, vars, null);
    }

    /**
     * Start a workflow given a workflow definition.
     */
    public WorkflowContext startWorkflow(String caseId, String engineName, WorkflowDefinition definition, Map<String, Object> variableMap) {
        WorkflowVariables vars = convertVariables(variableMap);
        RuntimeService rts = getRuntimeService(engineName);
        log.info("WorkflowEngine initialized using {}", engineName);
        return rts.startCase(caseId, definition, vars, null);
    }

    public WorkflowContext startWorkflow(String caseId, String engineName,  List<String> taskNames, WorkflowVariables variables) {
        WorkflowDefinition def = buildDefinition(taskNames);
        RuntimeService rts = getRuntimeService(engineName);
        log.info("WorkflowEngine initialized using {}", engineName);
        return rts.startCase(caseId, def, variables, null);
    }

    public WorkflowContext startWorkflow(String caseId, String engineName, WorkflowDefinition definition, WorkflowVariables variables) {
        RuntimeService rts = getRuntimeService(engineName);
        log.info("WorkflowEngine initialized using {}", engineName);
        return rts.startCase(caseId, definition, variables, null);
    }


    /**
     * Build workflow definition from a list of task names.
     */
    public WorkflowDefinition buildDefinition(List<String> taskNames) {
        if (taskNames == null || taskNames.isEmpty()) {
            throw new IllegalArgumentException("Task names list cannot be null or empty");
        }

        WorkflowDefinition def = new WorkflowDefinition();

        // First pass: validate all task names exist
        for (String taskName : taskNames) {
            getTaskOrThrow(taskName);
        }

        // Second pass: build workflow steps
        for (int i = 0; i < taskNames.size(); i++) {
            String current = taskNames.get(i);
            String next = i < taskNames.size() - 1 ? taskNames.get(i + 1) : null;

            TaskDescriptor td = getTaskOrThrow(current);

            Task step = new Task(
                    td.taskName(),
                    td.beanName(),
                    next,
                    null
            );

            def.addStep(step);
        }

        return def;
    }

    /**
     * Convert user-provided variables into WorkflowVariables.
     */
    private WorkflowVariables convertVariables(Map<String, Object> map) {
        WorkflowVariables vars = new WorkflowVariables();
        if (map == null) return vars;

        map.forEach((key, val) -> {
            vars.setValue(key, WorkflowVariableType.OBJECT, val);
        });

        return vars;
    }

    /**
     * Fetch and validate task.
     */
    private TaskDescriptor getTaskOrThrow(String taskName) {
        TaskDescriptor td = taskScanner.getByTaskName(taskName);
        if (td == null)
            throw new IllegalArgumentException("No @Task registered with name: " + taskName);
        return td;
    }


    private Entry<String, RuntimeService> getDefaultRuntimeService() {
        return runtimeServices.entrySet()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No RuntimeService available"));
    }


    /**
     * Get RuntimeService by name or throw exception with helpful message.
     */
    private RuntimeService getRuntimeService(String engineName) {
        RuntimeService rts = runtimeServices.get(engineName);
        if (rts == null) {
            throw new IllegalArgumentException(
                "No RuntimeService found for engine: '" + engineName + "'. " +
                "Available engines: " + runtimeServices.keySet());
        }
        return rts;
    }

}

