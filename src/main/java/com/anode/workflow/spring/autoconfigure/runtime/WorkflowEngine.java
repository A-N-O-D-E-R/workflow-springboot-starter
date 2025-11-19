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

/**
 * Facade for workflow execution operations.
 *
 * <p>Provides a simplified API for starting and managing workflows. This class integrates
 * with the Spring Boot configuration to automatically use the configured runtime services
 * and task registry.
 *
 * <p><b>Features:</b>
 * <ul>
 *   <li>Start workflows from task names or pre-built definitions</li>
 *   <li>Automatic task discovery via {@link TaskScanner}</li>
 *   <li>Support for multiple runtime services (configured engines)</li>
 *   <li>Fluent workflow builder for complex workflows</li>
 * </ul>
 *
 * <p><b>Example usage:</b>
 * <pre>
 * // Start a simple workflow
 * WorkflowContext ctx = engine.startWorkflow(
 *     "case-123",
 *     List.of("taskA", "taskB"),
 *     Map.of("userId", 42)
 * );
 *
 * // Build a complex workflow
 * FluentWorkflowBuilder builder = engine.builder();
 * builder.addTask("processOrder")
 *        .addTask("sendNotification")
 *        .addVariable("orderId", 123);
 * WorkflowContext ctx = engine.startWorkflow("case-456", builder);
 * </pre>
 *
 * @see RuntimeService
 * @see TaskScanner
 * @see FluentWorkflowBuilder
 */
public class WorkflowEngine {
      private static final String START_STEP = "start";
    private static final String END_STEP = "end";

    private static final Logger log =
            LoggerFactory.getLogger(WorkflowEngine.class);

    private final Map<String, RuntimeService> runtimeServices;
    private final TaskScanner taskScanner;

    /**
     * Constructs a WorkflowEngine with the given runtime services and task scanner.
     *
     * @param services map of engine names to runtime services
     * @param taskScanner the task scanner for task discovery
     */
    public WorkflowEngine(Map<String, RuntimeService> services, TaskScanner taskScanner) {
        this.runtimeServices = services;
        this.taskScanner = taskScanner;
    }

    // ------------------------------------------------------------
    // PUBLIC API
    // ------------------------------------------------------------

    /**
     * Start a workflow given a sequence of task names.
     *
     * @param caseId unique case identifier (cannot be null or empty)
     * @param beanNames list of task bean names
     * @param variableMap workflow variables
     * @return workflow context
     * @throws IllegalArgumentException if caseId is null/empty or beanNames is invalid
     */
    public WorkflowContext startWorkflow(String caseId, List<String> beanNames, Map<String, Object> variableMap) {
        validateCaseId(caseId);
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
        validateCaseId(caseId);
        WorkflowVariables vars = convertVariables(variableMap);
        Entry<String, RuntimeService> rts = getDefaultRuntimeService();
        log.info("WorkflowEngine initialized using {}", rts.getKey());
        return rts.getValue().startCase(caseId, definition, vars, null);
    }

    public WorkflowContext startWorkflow(String caseId,  List<String> beanNames, WorkflowVariables variables) {
        validateCaseId(caseId);
        WorkflowDefinition def = buildDefinition(beanNames);
        Entry<String, RuntimeService> rts = getDefaultRuntimeService();
        log.info("WorkflowEngine initialized using {}", rts.getKey());
        return rts.getValue().startCase(caseId, def, variables, null);
    }

    public WorkflowContext startWorkflow(String caseId, WorkflowDefinition definition, WorkflowVariables variables) {
        validateCaseId(caseId);
        Entry<String, RuntimeService> rts = getDefaultRuntimeService();
        log.info("WorkflowEngine initialized using {}", rts.getKey());
        return rts.getValue().startCase(caseId, definition, variables, null);
    }


    public WorkflowContext startWorkflow(String caseId, String engineName, List<String> beanNames, Map<String, Object> variableMap) {
        validateCaseId(caseId);
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
        validateCaseId(caseId);
        WorkflowVariables vars = convertVariables(variableMap);
        RuntimeService rts = getRuntimeService(engineName);
        log.info("WorkflowEngine initialized using {}", engineName);
        return rts.startCase(caseId, definition, vars, null);
    }

    public WorkflowContext startWorkflow(String caseId, String engineName,  List<String> beanNames, WorkflowVariables variables) {
        validateCaseId(caseId);
        WorkflowDefinition def = buildDefinition(beanNames);
        RuntimeService rts = getRuntimeService(engineName);
        log.info("WorkflowEngine initialized using {}", engineName);
        return rts.startCase(caseId, def, variables, null);
    }

    public WorkflowContext startWorkflow(String caseId, String engineName, WorkflowDefinition definition, WorkflowVariables variables) {
        validateCaseId(caseId);
        RuntimeService rts = getRuntimeService(engineName);
        log.info("WorkflowEngine initialized using {}", engineName);
        return rts.startCase(caseId, definition, variables, null);
    }


    /**
     * Build workflow definition from a list of bean names.
     *
     * @param beanNames list of task bean names
     * @return workflow definition
     * @throws IllegalArgumentException if beanNames is null, empty, contains null/empty entries, or has duplicates
     */
    public WorkflowDefinition buildDefinition(List<String> beanNames) {
        // Validate list is not null or empty
        if (beanNames == null || beanNames.isEmpty()) {
            throw new IllegalArgumentException("Task names list cannot be null or empty");
        }

        // Track seen bean names to detect duplicates
        java.util.Set<String> seenNames = new java.util.HashSet<>();

        // First pass: validate all bean names
        for (int i = 0; i < beanNames.size(); i++) {
            String beanName = beanNames.get(i);

            // Check for null or empty
            if (beanName == null || beanName.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("Task name at index %d cannot be null or empty", i));
            }

            // Check for duplicates
            if (seenNames.contains(beanName)) {
                throw new IllegalArgumentException(
                    String.format("Duplicate task name '%s' found at index %d. Each task must be unique in the workflow.",
                                  beanName, i));
            }
            seenNames.add(beanName);

            // Validate bean exists in registry
            getTaskOrThrow(beanName);
        }

        WorkflowDefinition def = new WorkflowDefinition();

        // Second pass: build workflow steps
        for (int i = 0; i < beanNames.size(); i++) {
            String current = beanNames.get(i);
            String next = i < beanNames.size() - 1 ? beanNames.get(i + 1) : END_STEP; // Workflow ask for the last step to be name END_STEP"
            TaskDescriptor td = getTaskOrThrow(current);
            Task step = new Task(
                    i==0?START_STEP:td.taskName(), // Workflow ask for the first step to be START_STEP
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
     * Validate case ID is not null or empty.
     *
     * @param caseId the case ID to validate
     * @throws IllegalArgumentException if caseId is null or empty
     */
    private void validateCaseId(String caseId) {
        if (caseId == null || caseId.trim().isEmpty()) {
            throw new IllegalArgumentException("Case ID cannot be null or empty");
        }
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

