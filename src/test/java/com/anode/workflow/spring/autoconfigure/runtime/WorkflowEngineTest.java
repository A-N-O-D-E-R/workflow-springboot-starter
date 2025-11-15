package com.anode.workflow.spring.autoconfigure.runtime;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.responses.StepResponseType;
import com.anode.workflow.entities.steps.responses.TaskResponse;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.annotations.Task;
import com.anode.workflow.spring.autoconfigure.scanner.TaskScanner;
import com.anode.workflow.spring.autoconfigure.scanner.TaskScanner.TaskDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Tests for WorkflowEngine component.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private RuntimeService runtimeService2;

    private TaskScanner taskScanner;
    private WorkflowEngine workflowEngine;

    @BeforeEach
    void setUp() {
        GenericApplicationContext context = createContext();
        registerTaskBean(context, "testTask", TestTask.class);
        registerTaskBean(context, "secondTask", SecondTask.class);
        context.refresh();

        taskScanner = new TaskScanner(context);
        taskScanner.init();

        Map<String, RuntimeService> services = new HashMap<>();
        services.put("default-engine", runtimeService);
        services.put("secondary-engine", runtimeService2);

        workflowEngine = new WorkflowEngine(services, taskScanner);

        // Setup mock to return a dummy context (lenient to avoid unnecessary stubbing errors)
        WorkflowContext mockContext = mock(WorkflowContext.class);
        lenient().when(runtimeService.startCase(any(), any(), any(), any())).thenReturn(mockContext);
        lenient().when(runtimeService2.startCase(any(), any(), any(), any())).thenReturn(mockContext);
    }

    @Test
    void shouldStartWorkflowWithTaskNames() {
        List<String> taskNames = Arrays.asList("testTask");
        Map<String, Object> variables = Map.of("key", "value");

        WorkflowContext result = workflowEngine.startWorkflow("case-123", taskNames, variables);

        assertThat(result).isNotNull();
        verify(runtimeService).startCase(eq("case-123"), any(WorkflowDefinition.class),
            any(WorkflowVariables.class), eq(null));
    }

    @Test
    void shouldStartWorkflowWithDefinitionAndVariableMap() {
        WorkflowDefinition definition = new WorkflowDefinition();
        Map<String, Object> variables = Map.of("key", "value");

        WorkflowContext result = workflowEngine.startWorkflow("case-123", definition, variables);

        assertThat(result).isNotNull();
        verify(runtimeService).startCase(eq("case-123"), eq(definition),
            any(WorkflowVariables.class), eq(null));
    }

    @Test
    void shouldStartWorkflowWithTaskNamesAndWorkflowVariables() {
        List<String> taskNames = Arrays.asList("testTask");
        WorkflowVariables variables = new WorkflowVariables();

        WorkflowContext result = workflowEngine.startWorkflow("case-123", taskNames, variables);

        assertThat(result).isNotNull();
        verify(runtimeService).startCase(eq("case-123"), any(WorkflowDefinition.class),
            eq(variables), eq(null));
    }

    @Test
    void shouldStartWorkflowWithDefinitionAndWorkflowVariables() {
        WorkflowDefinition definition = new WorkflowDefinition();
        WorkflowVariables variables = new WorkflowVariables();

        WorkflowContext result = workflowEngine.startWorkflow("case-123", definition, variables);

        assertThat(result).isNotNull();
        verify(runtimeService).startCase(eq("case-123"), eq(definition), eq(variables), eq(null));
    }

    @Test
    void shouldStartWorkflowWithSpecificEngine() {
        List<String> taskNames = Arrays.asList("testTask");
        Map<String, Object> variables = Map.of("key", "value");

        WorkflowContext result = workflowEngine.startWorkflow("case-123", "secondary-engine",
            taskNames, variables);

        assertThat(result).isNotNull();
        verify(runtimeService2).startCase(eq("case-123"), any(WorkflowDefinition.class),
            any(WorkflowVariables.class), eq(null));
        verifyNoInteractions(runtimeService);
    }

    @Test
    void shouldStartWorkflowWithSpecificEngineAndDefinition() {
        WorkflowDefinition definition = new WorkflowDefinition();
        Map<String, Object> variables = Map.of("key", "value");

        WorkflowContext result = workflowEngine.startWorkflow("case-123", "secondary-engine",
            definition, variables);

        assertThat(result).isNotNull();
        verify(runtimeService2).startCase(eq("case-123"), eq(definition),
            any(WorkflowVariables.class), eq(null));
    }

    @Test
    void shouldStartWorkflowWithSpecificEngineAndWorkflowVariables() {
        List<String> taskNames = Arrays.asList("testTask");
        WorkflowVariables variables = new WorkflowVariables();

        WorkflowContext result = workflowEngine.startWorkflow("case-123", "secondary-engine",
            taskNames, variables);

        assertThat(result).isNotNull();
        verify(runtimeService2).startCase(eq("case-123"), any(WorkflowDefinition.class),
            eq(variables), eq(null));
    }

    @Test
    void shouldStartWorkflowWithSpecificEngineDefinitionAndVariables() {
        WorkflowDefinition definition = new WorkflowDefinition();
        WorkflowVariables variables = new WorkflowVariables();

        WorkflowContext result = workflowEngine.startWorkflow("case-123", "secondary-engine",
            definition, variables);

        assertThat(result).isNotNull();
        verify(runtimeService2).startCase(eq("case-123"), eq(definition), eq(variables), eq(null));
    }

    @Test
    void shouldBuildDefinitionWithSingleTask() {
        List<String> taskNames = Arrays.asList("testTask");

        WorkflowDefinition definition = workflowEngine.buildDefinition(taskNames);

        assertThat(definition).isNotNull();
        assertThat(definition.getSteps()).hasSize(1);
    }

    @Test
    void shouldBuildDefinitionWithMultipleTasks() {
        List<String> taskNames = Arrays.asList("testTask", "secondTask");

        WorkflowDefinition definition = workflowEngine.buildDefinition(taskNames);

        assertThat(definition).isNotNull();
        assertThat(definition.getSteps()).hasSize(2);
    }

    @Test
    void shouldThrowExceptionForNullTaskNames() {
        assertThatThrownBy(() -> workflowEngine.buildDefinition(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task names list cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForEmptyTaskNames() {
        assertThatThrownBy(() -> workflowEngine.buildDefinition(Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task names list cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForNonExistentTask() {
        List<String> taskNames = Arrays.asList("nonExistentTask");

        assertThatThrownBy(() -> workflowEngine.buildDefinition(taskNames))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No @Task registered with bean name: nonExistentTask");
    }

    @Test
    void shouldThrowExceptionForNonExistentEngine() {
        List<String> taskNames = Arrays.asList("testTask");
        Map<String, Object> variables = Map.of("key", "value");

        assertThatThrownBy(() -> workflowEngine.startWorkflow("case-123", "non-existent-engine",
            taskNames, variables))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No RuntimeService found for engine: 'non-existent-engine'")
            .hasMessageContaining("Available engines:");
    }

    @Test
    void shouldThrowExceptionWhenNoRuntimeServiceAvailable() {
        Map<String, RuntimeService> emptyServices = new HashMap<>();
        WorkflowEngine emptyEngine = new WorkflowEngine(emptyServices, taskScanner);

        List<String> taskNames = Arrays.asList("testTask");
        Map<String, Object> variables = Map.of("key", "value");

        assertThatThrownBy(() -> emptyEngine.startWorkflow("case-123", taskNames, variables))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No RuntimeService available");
    }

    @Test
    void shouldConvertVariablesCorrectly() {
        List<String> taskNames = Arrays.asList("testTask");
        Map<String, Object> variables = new HashMap<>();
        variables.put("string", "value");
        variables.put("number", 42);
        variables.put("boolean", true);

        workflowEngine.startWorkflow("case-123", taskNames, variables);

        ArgumentCaptor<WorkflowVariables> captor = ArgumentCaptor.forClass(WorkflowVariables.class);
        verify(runtimeService).startCase(eq("case-123"), any(WorkflowDefinition.class),
            captor.capture(), eq(null));

        WorkflowVariables captured = captor.getValue();
        assertThat(captured).isNotNull();
    }

    @Test
    void shouldHandleNullVariablesMap() {
        List<String> taskNames = Arrays.asList("testTask");
        Map<String, Object> variables = null;

        workflowEngine.startWorkflow("case-123", taskNames, variables);

        ArgumentCaptor<WorkflowVariables> captor = ArgumentCaptor.forClass(WorkflowVariables.class);
        verify(runtimeService).startCase(eq("case-123"), any(WorkflowDefinition.class),
            captor.capture(), eq(null));

        WorkflowVariables captured = captor.getValue();
        assertThat(captured).isNotNull();
    }

    // Helper methods

    private GenericApplicationContext createContext() {
        GenericApplicationContext context = new GenericApplicationContext();
        MockEnvironment env = new MockEnvironment();
        env.setProperty("workflow.scan-base-package", "com.anode.workflow.spring.autoconfigure.runtime");
        context.setEnvironment(env);
        return context;
    }

    private void registerTaskBean(GenericApplicationContext context, String beanName, Class<?> beanClass) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
        context.registerBeanDefinition(beanName, beanDefinition);
    }

    // Test task classes

    @Task
    public static class TestTask implements InvokableTask {
        @Override
        public TaskResponse executeStep() {
            return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
        }
    }

    @Task
    public static class SecondTask implements InvokableTask {
        @Override
        public TaskResponse executeStep() {
            return new TaskResponse(StepResponseType.OK_PROCEED, null, null);
        }
    }
}
