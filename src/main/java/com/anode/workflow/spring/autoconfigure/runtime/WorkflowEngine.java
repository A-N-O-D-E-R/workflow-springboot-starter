package com.anode.workflow.spring.autoconfigure.runtime;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.anode.workflow.entities.steps.Branch;
import com.anode.workflow.entities.steps.InvokableRoute;
import com.anode.workflow.entities.steps.Join;
import com.anode.workflow.entities.steps.Route;
import com.anode.workflow.entities.steps.Step;
import com.anode.workflow.entities.steps.Step.StepType;
import com.anode.workflow.entities.steps.Task;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.entities.workflows.WorkflowVariable.WorkflowVariableType;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.model.WorkflowNode;
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
    protected static final String START_STEP = "start";
    protected static final String END_STEP = "end";

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);

    private final WorkflowRuntimes runtimes;
    private final TaskScanner taskScanner;
    private ApplicationContext applicationContext;

    /**
     * Constructs a WorkflowEngine over the configured engines.
     *
     * @param runtimes the configured engines
     * @param taskScanner the task scanner for task discovery
     */
    public WorkflowEngine(WorkflowRuntimes runtimes, TaskScanner taskScanner) {
        this.runtimes = runtimes;
        this.taskScanner = taskScanner;
    }

    /**
     * Sets the ApplicationContext for route bean lookup.
     * This is optional and only needed if route validation is required during definition building.
     *
     * @param applicationContext the Spring application context
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // ------------------------------------------------------------
    // PUBLIC API — every operation uses a new RuntimeService (see WorkflowRuntimes)
    // ------------------------------------------------------------

    /** Starts a case on the default engine from a sequence of {@code @Task} bean names. */
    public WorkflowContext startWorkflow(String caseId, List<String> beanNames, Map<String, Object> variableMap) {
        return startWorkflow(caseId, runtimes.defaultEngine(), buildDefinition(beanNames), convertVariables(variableMap));
    }

    /** Starts a case on the default engine. */
    public WorkflowContext startWorkflow(String caseId, WorkflowDefinition definition, Map<String, Object> variableMap) {
        return startWorkflow(caseId, runtimes.defaultEngine(), definition, convertVariables(variableMap));
    }

    /** Starts a case on the default engine from a sequence of {@code @Task} bean names. */
    public WorkflowContext startWorkflow(String caseId, List<String> beanNames, WorkflowVariables variables) {
        return startWorkflow(caseId, runtimes.defaultEngine(), buildDefinition(beanNames), variables);
    }

    /** Starts a case on the default engine. */
    public WorkflowContext startWorkflow(String caseId, WorkflowDefinition definition, WorkflowVariables variables) {
        return startWorkflow(caseId, runtimes.defaultEngine(), definition, variables);
    }

    /** Starts a case on the named engine from a sequence of {@code @Task} bean names. */
    public WorkflowContext startWorkflow(String caseId, String engineName, List<String> beanNames, Map<String, Object> variableMap) {
        return startWorkflow(caseId, engineName, buildDefinition(beanNames), convertVariables(variableMap));
    }

    /** Starts a case on the named engine. */
    public WorkflowContext startWorkflow(String caseId, String engineName, WorkflowDefinition definition, Map<String, Object> variableMap) {
        return startWorkflow(caseId, engineName, definition, convertVariables(variableMap));
    }

    /** Starts a case on the named engine from a sequence of {@code @Task} bean names. */
    public WorkflowContext startWorkflow(String caseId, String engineName, List<String> beanNames, WorkflowVariables variables) {
        return startWorkflow(caseId, engineName, buildDefinition(beanNames), variables);
    }

    /**
     * Starts a case on the named engine: it runs until it pends (a step returned a pend, a pause)
     * or ends.
     */
    public WorkflowContext startWorkflow(String caseId, String engineName, WorkflowDefinition definition, WorkflowVariables variables) {
        validateCaseId(caseId);
        logger.debug("Starting case {} on engine {}", caseId, engineName);
        return runtimes.runtime(engineName).startCase(caseId, definition, variables, null);
    }

    /** Resumes a pended case of the default engine. */
    public WorkflowContext resumeWorkflow(String caseId) {
        return resumeWorkflow(caseId, runtimes.defaultEngine(), null);
    }

    /** Resumes a pended case of the default engine, first merging {@code variables} into it. */
    public WorkflowContext resumeWorkflow(String caseId, Map<String, Object> variables) {
        return resumeWorkflow(caseId, runtimes.defaultEngine(), convertVariables(variables));
    }

    /**
     * Resumes a pended case of the named engine; {@code variables} (may be null) are merged into the
     * case before it continues.
     */
    public WorkflowContext resumeWorkflow(String caseId, String engineName, WorkflowVariables variables) {
        validateCaseId(caseId);
        logger.debug("Resuming case {} on engine {}", caseId, engineName);
        return variables == null
                ? runtimes.runtime(engineName).resumeCase(caseId)
                : runtimes.runtime(engineName).resumeCase(caseId, variables);
    }

    /**
     * Reopens a completed case of the named engine at the step of {@code ticket}; with {@code
     * pendBeforeResume} it pends in {@code pendWorkBasket} instead of running immediately.
     */
    public WorkflowContext reopenWorkflow(
            String caseId, String engineName, String ticket, boolean pendBeforeResume, String pendWorkBasket,
            WorkflowVariables variables) {
        validateCaseId(caseId);
        return runtimes.runtime(engineName).reopenCase(caseId, ticket, pendBeforeResume, pendWorkBasket, variables);
    }

    /** Whether a case with this id exists on the named engine. */
    public boolean isStarted(String caseId, String engineName) {
        validateCaseId(caseId);
        return runtimes.runtime(engineName).isCaseStarted(caseId);
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
            // steps are named after their bean (unique in the context), "next" refers to those
            // names; the engine requires the first step to be named START_STEP
            Task step = new Task(i == 0 ? START_STEP : td.beanName(), td.beanName(), next, null);

            def.addStep(step);
        }

        return def;
    }

    /**
     * Build workflow definition from a list of workflow nodes.
     *
     * <p>This method processes {@link WorkflowNode} objects to create a complete workflow definition.
     * It supports various node types including tasks, routes (sequential and parallel), and joins.
     *
     * @param nodes list of workflow nodes (can include tasks, routes, joins)
     * @return workflow definition
     * @throws IllegalArgumentException if nodes is null, empty, contains null entries, or has invalid node configurations
     */
    public WorkflowDefinition buildDefinitionFromNodes(List<WorkflowNode> nodes) {
        // Validate list is not null or empty
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Workflow nodes list cannot be null or empty");
        }

        // Track seen node names to detect duplicates and validate references
        java.util.Set<String> nodeNames = new java.util.HashSet<>();

        // First pass: collect all node names and validate basic structure
        for (int i = 0; i < nodes.size(); i++) {
            WorkflowNode node = nodes.get(i);

            // Check for null
            if (node == null) {
                throw new IllegalArgumentException(
                    String.format("Workflow node at index %d cannot be null", i));
            }

            // Check for null or empty name
            if (node.getName() == null || node.getName().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("Workflow node at index %d must have a non-null, non-empty name", i));
            }

            // Check for duplicates
            if (nodeNames.contains(node.getName())) {
                throw new IllegalArgumentException(
                    String.format("Duplicate workflow node name '%s' found at index %d. Each node must be unique in the workflow.",
                                  node.getName(), i));
            }
            nodeNames.add(node.getName());
        }

        // Second pass: validate node configurations and references
        for (int i = 0; i < nodes.size(); i++) {
            WorkflowNode node = nodes.get(i);
            validateNode(node, i, nodeNames);
        }

        WorkflowDefinition def = new WorkflowDefinition();

        // Third pass: build workflow steps
        for (int i = 0; i < nodes.size(); i++) {
            WorkflowNode node = nodes.get(i);
            Step step = createStep(node, i == 0);
            def.addStep(step);
        }

        return def;
    }

    /**
     * Validate a workflow node based on its type.
     *
     * @param node the workflow node to validate
     * @param index the index of the node in the list
     * @param nodeNames set of all valid node names for reference validation
     */
    private void validateNode(WorkflowNode node, int index, java.util.Set<String> nodeNames) {
        StepType type = node.getType();

        // Validate 'next' reference if present
        if (node.getNext() != null && !node.getNext().trim().isEmpty()) {
            // Allow special workflow step names (START_STEP, END_STEP) or valid node names
            if (!nodeNames.contains(node.getNext()) &&
                !node.getNext().equals(START_STEP) &&
                !node.getNext().equals(END_STEP)) {
                throw new IllegalArgumentException(
                    String.format("Node '%s' at index %d references invalid 'next' node: '%s'",
                                  node.getName(), index, node.getNext()));
            }
        }

        // For routes, validate branches exist
        if (type == StepType.S_ROUTE || type == StepType.P_ROUTE) {
            if (node.getBranches() == null || node.getBranches().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("Route node '%s' at index %d must have at least one branch",
                                  node.getName(), index));
            }

            // Validate component exists for routes
            if (node.getComponent() == null || node.getComponent().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("Route node '%s' at index %d must have a component specified",
                                  node.getName(), index));
            }

            // Validate the route component is registered as an InvokableRoute bean
            validateRouteComponent(node.getComponent(), node.getName(), index);

            // Validate each branch
            for (int j = 0; j < node.getBranches().size(); j++) {
                WorkflowNode.Branch branch = node.getBranches().get(j);
                if (branch == null) {
                    throw new IllegalArgumentException(
                        String.format("Route node '%s' at index %d has null branch at position %d",
                                      node.getName(), index, j));
                }
                if (branch.name == null || branch.name.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        String.format("Route node '%s' at index %d has branch at position %d with null or empty name",
                                      node.getName(), index, j));
                }
                // Validate branch 'next' reference
                if (branch.next == null || branch.next.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        String.format("Route node '%s' at index %d has branch '%s' at position %d with null or empty 'next' reference",
                                      node.getName(), index, branch.name, j));
                }
                // Allow special workflow step names (START_STEP, END_STEP) or valid node names
                if (!nodeNames.contains(branch.next) &&
                    !branch.next.equals(START_STEP) &&
                    !branch.next.equals(END_STEP)) {
                    throw new IllegalArgumentException(
                        String.format("Route node '%s' at index %d has branch '%s' at position %d referencing invalid node: '%s'",
                                      node.getName(), index, branch.name, j, branch.next));
                }
            }
        }
        // For tasks (null type or explicitly TASK type), validate component exists
        else if (type == null || type == StepType.TASK) {
            if (node.getComponent() == null || node.getComponent().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("Task node '%s' at index %d must have a component specified",
                                  node.getName(), index));
            }

            // Validate the component is registered
            getTaskOrThrow(node.getComponent());
        }
        // For joins, no component validation needed
        else if (type == StepType.P_JOIN) {
            // Joins don't need a component
        }
        else {
            throw new IllegalArgumentException(
                String.format("Unsupported node type '%s' for node '%s' at index %d. " +
                              "Supported types are: TASK, S_ROUTE, P_ROUTE, P_JOIN",
                              type, node.getName(), index));
        }
    }

    /**
     * Validate that a route component is registered as an InvokableRoute bean.
     * Falls back to task validation if ApplicationContext is not available.
     *
     * @param componentName the bean name to validate
     * @param nodeName the name of the workflow node (for error messages)
     * @param index the index of the node (for error messages)
     */
    private void validateRouteComponent(String componentName, String nodeName, int index) {
        if (applicationContext != null) {
            try {
                // Try to get the bean as InvokableRoute
                InvokableRoute route = applicationContext.getBean(componentName, InvokableRoute.class);
                if (route == null) {
                    throw new IllegalArgumentException(
                        String.format("Route node '%s' at index %d references component '%s' which is not an InvokableRoute bean",
                                      nodeName, index, componentName));
                }
            } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
                throw new IllegalArgumentException(
                    String.format("Route node '%s' at index %d references component '%s' which is not registered as an InvokableRoute bean. " +
                                  "Make sure the route bean exists in the application context.",
                                  nodeName, index, componentName), e);
            }
        } else {
            // Fallback: validate as task (for backward compatibility or when ApplicationContext is not set)
            logger.warn("ApplicationContext not available for route validation. Component '{}' will be validated at runtime.", componentName);
            getTaskOrThrow(componentName);
        }
    }

    /**
     * Create a workflow step from a workflow node.
     */
    private Step createStep(WorkflowNode node, boolean isFirst) {
        StepType type = node.getType();
        String stepName = isFirst ? START_STEP : node.getName();
        String next = node.getNext() != null ? node.getNext() : END_STEP;

        // Handle routes (sequential or parallel)
        if (type == StepType.S_ROUTE || type == StepType.P_ROUTE) {
            Map<String, Branch> branches = new java.util.HashMap<>();
            for (WorkflowNode.Branch wfBranch : node.getBranches()) {
                // Create a Branch object from workflow-lib
                Branch branch = new Branch(wfBranch.name, wfBranch.next);
                branches.put(wfBranch.name, branch);
            }

            return new Route(
                stepName,
                node.getComponent(),
                next,
                branches,
                type
            );
        }
        // Handle parallel join
        else if (type == StepType.P_JOIN) {
            return new Join(stepName, next);
        }
        // Handle regular task (null type or TASK type)
        else {
            return new Task(
                stepName,
                node.getComponent(),
                next,
                null
            );
        }
    }

    /**
     * Convert user-provided variables into WorkflowVariables.
     */
    private WorkflowVariables convertVariables(Map<String, Object> map) {
        WorkflowVariables vars = new WorkflowVariables();
        if (map == null) return vars;

        map.forEach((key, val) -> vars.setValue(key, typeOf(val), val));
        return vars;
    }

    /** The most specific variable type for a value, so it persists as readable text when possible. */
    static WorkflowVariableType typeOf(Object value) {
        return switch (value) {
            case String s -> WorkflowVariableType.STRING;
            case Boolean b -> WorkflowVariableType.BOOLEAN;
            case Integer i -> WorkflowVariableType.INTEGER;
            case Long l -> WorkflowVariableType.LONG;
            case List<?> list when !list.isEmpty() && list.stream().allMatch(String.class::isInstance) ->
                    WorkflowVariableType.LIST_OF_STRING;
            case List<?> list when !list.isEmpty() && list.stream().allMatch(Boolean.class::isInstance) ->
                    WorkflowVariableType.LIST_OF_BOOLEAN;
            case List<?> list when !list.isEmpty() && list.stream().allMatch(Integer.class::isInstance) ->
                    WorkflowVariableType.LIST_OF_INTEGER;
            case List<?> list when !list.isEmpty() && list.stream().allMatch(Long.class::isInstance) ->
                    WorkflowVariableType.LIST_OF_LONG;
            case List<?> list -> WorkflowVariableType.LIST_OF_OBJECT;
            case null, default -> WorkflowVariableType.OBJECT;
        };
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


}

