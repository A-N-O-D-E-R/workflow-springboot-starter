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
    public WorkflowContext startWorkflow(String caseId, List<String> beanNames, Map<String, Object> variableMap) {
        WorkflowDefinition def = buildDefinition(beanNames);
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

    public WorkflowContext startWorkflow(String caseId,  List<String> beanNames, WorkflowVariables variables) {
        WorkflowDefinition def = buildDefinition(beanNames);
        Entry<String, RuntimeService> rts = getDefaultRuntimeService();
        log.info("WorkflowEngine initialized using {}", rts.getKey());
        return rts.getValue().startCase(caseId, def, variables, null);
    }

    public WorkflowContext startWorkflow(String caseId, WorkflowDefinition definition, WorkflowVariables variables) {
        Entry<String, RuntimeService> rts = getDefaultRuntimeService();
        log.info("WorkflowEngine initialized using {}", rts.getKey());
        return rts.getValue().startCase(caseId, definition, variables, null);
    }


    public WorkflowContext startWorkflow(String caseId, String engineName, List<String> beanNames, Map<String, Object> variableMap) {
        WorkflowDefinition def = buildDefinition(beanNames);
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

    public WorkflowContext startWorkflow(String caseId, String engineName,  List<String> beanNames, WorkflowVariables variables) {
        WorkflowDefinition def = buildDefinition(beanNames);
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
     * Build workflow definition from a list of bean names.
     */
    public WorkflowDefinition buildDefinition(List<String> beanNames) {
        if (beanNames == null || beanNames.isEmpty()) {
            throw new IllegalArgumentException("Task names list cannot be null or empty");
        }

        WorkflowDefinition def = new WorkflowDefinition();

        // First pass: validate all bean names exist
        for (String beanName : beanNames) {
            getTaskOrThrow(beanName);
        }

        // Second pass: build workflow steps
        for (int i = 0; i < beanNames.size(); i++) {
            String current = beanNames.get(i);
            String next = i < beanNames.size() - 1 ? beanNames.get(i + 1) : "end"; // Workflow ask for the last step to be name "end"
            TaskDescriptor td = getTaskOrThrow(current);
            Task step = new Task(
                    i==0?"start":td.taskName(), // Workflow ask for the first step to be "start"
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
    private TaskDescriptor getTaskOrThrow(String bean) {
        TaskDescriptor td = taskScanner.getByBeanName(bean);
        if (td == null)
            throw new IllegalArgumentException("No @Task registered with bean name: " + bean);
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

