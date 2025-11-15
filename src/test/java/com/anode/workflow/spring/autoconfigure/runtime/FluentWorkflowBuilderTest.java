package com.anode.workflow.spring.autoconfigure.runtime;

import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.entities.workflows.WorkflowDefinition;
import com.anode.workflow.entities.workflows.WorkflowVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Tests for FluentWorkflowBuilder component.
 */
@ExtendWith(MockitoExtension.class)
class FluentWorkflowBuilderTest {

    @Mock
    private WorkflowEngine workflowEngine;

    @Mock
    private WorkflowContext mockContext;

    private FluentWorkflowBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new FluentWorkflowBuilder(workflowEngine, "case-123");
        lenient().when(workflowEngine.startWorkflow(anyString(), anyList(), anyMap())).thenReturn(mockContext);
        lenient().when(workflowEngine.startWorkflow(anyString(), any(WorkflowDefinition.class), anyMap())).thenReturn(mockContext);
        lenient().when(workflowEngine.startWorkflow(anyString(), anyList(), any(WorkflowVariables.class))).thenReturn(mockContext);
        lenient().when(workflowEngine.startWorkflow(anyString(), any(WorkflowDefinition.class), any(WorkflowVariables.class))).thenReturn(mockContext);
        lenient().when(workflowEngine.startWorkflow(anyString(), anyString(), anyList(), anyMap())).thenReturn(mockContext);
        lenient().when(workflowEngine.startWorkflow(anyString(), anyString(), any(WorkflowDefinition.class), anyMap())).thenReturn(mockContext);
        lenient().when(workflowEngine.startWorkflow(anyString(), anyString(), anyList(), any(WorkflowVariables.class))).thenReturn(mockContext);
        lenient().when(workflowEngine.startWorkflow(anyString(), anyString(), any(WorkflowDefinition.class), any(WorkflowVariables.class))).thenReturn(mockContext);
    }

    @Test
    void shouldBuildAndStartWorkflowWithSingleTask() {
        WorkflowContext result = builder
            .task("task1")
            .start();

        assertThat(result).isEqualTo(mockContext);
        verify(workflowEngine).startWorkflow(eq("case-123"), eq(Arrays.asList("task1")), anyMap());
    }

    @Test
    void shouldBuildAndStartWorkflowWithMultipleTasks() {
        WorkflowContext result = builder
            .task("task1")
            .task("task2")
            .task("task3")
            .start();

        assertThat(result).isEqualTo(mockContext);
        verify(workflowEngine).startWorkflow(eq("case-123"),
            eq(Arrays.asList("task1", "task2", "task3")), anyMap());
    }

    @Test
    void shouldAddTasksInBulk() {
        List<String> tasks = Arrays.asList("task1", "task2", "task3");

        WorkflowContext result = builder
            .tasks(tasks)
            .start();

        assertThat(result).isEqualTo(mockContext);
        verify(workflowEngine).startWorkflow(eq("case-123"), eq(tasks), anyMap());
    }

    @Test
    void shouldAddSingleVariable() {
        WorkflowContext result = builder
            .task("task1")
            .variable("key", "value")
            .start();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(workflowEngine).startWorkflow(eq("case-123"), anyList(), captor.capture());

        Map<String, Object> variables = captor.getValue();
        assertThat(variables).containsEntry("key", "value");
    }

    @Test
    void shouldAddMultipleVariables() {
        WorkflowContext result = builder
            .task("task1")
            .variable("key1", "value1")
            .variable("key2", 42)
            .variable("key3", true)
            .start();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(workflowEngine).startWorkflow(eq("case-123"), anyList(), captor.capture());

        Map<String, Object> variables = captor.getValue();
        assertThat(variables)
            .containsEntry("key1", "value1")
            .containsEntry("key2", 42)
            .containsEntry("key3", true);
    }

    @Test
    void shouldAddVariablesInBulk() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("key1", "value1");
        vars.put("key2", "value2");

        WorkflowContext result = builder
            .task("task1")
            .variables(vars)
            .start();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(workflowEngine).startWorkflow(eq("case-123"), anyList(), captor.capture());

        Map<String, Object> variables = captor.getValue();
        assertThat(variables)
            .containsEntry("key1", "value1")
            .containsEntry("key2", "value2");
    }

    @Test
    void shouldSelectSpecificEngine() {
        WorkflowContext result = builder
            .engine("custom-engine")
            .task("task1")
            .start();

        assertThat(result).isEqualTo(mockContext);
        verify(workflowEngine).startWorkflow(eq("case-123"), eq("custom-engine"),
            eq(Arrays.asList("task1")), anyMap());
    }

    @Test
    void shouldBuildDefinitionWithoutStarting() {
        WorkflowDefinition mockDefinition = new WorkflowDefinition();
        when(workflowEngine.buildDefinition(anyList())).thenReturn(mockDefinition);

        WorkflowDefinition result = builder
            .task("task1")
            .task("task2")
            .buildDefinition();

        assertThat(result).isEqualTo(mockDefinition);
        verify(workflowEngine).buildDefinition(eq(Arrays.asList("task1", "task2")));
        verifyNoMoreInteractions(workflowEngine);
    }

    @Test
    void shouldBuildVariablesWithoutStarting() {
        WorkflowVariables result = builder
            .variable("key1", "value1")
            .variable("key2", 42)
            .buildVariables();

        assertThat(result).isNotNull();
        verifyNoInteractions(workflowEngine);
    }

    @Test
    void shouldStartWithPrebuiltDefinitionAndVariables() {
        WorkflowDefinition definition = new WorkflowDefinition();
        WorkflowVariables variables = new WorkflowVariables();

        WorkflowContext result = builder.start(definition, variables);

        assertThat(result).isEqualTo(mockContext);
        verify(workflowEngine).startWorkflow(eq("case-123"), eq(definition), eq(variables));
    }

    @Test
    void shouldStartWithPrebuiltDefinitionAndVariablesOnSpecificEngine() {
        WorkflowDefinition definition = new WorkflowDefinition();
        WorkflowVariables variables = new WorkflowVariables();

        WorkflowContext result = builder
            .engine("custom-engine")
            .start(definition, variables);

        assertThat(result).isEqualTo(mockContext);
        verify(workflowEngine).startWorkflow(eq("case-123"), eq("custom-engine"),
            eq(definition), eq(variables));
    }

    @Test
    void shouldChainMethodCallsFluentStyle() {
        WorkflowContext result = builder
            .engine("custom-engine")
            .task("task1")
            .task("task2")
            .variable("key1", "value1")
            .variable("key2", 42)
            .start();

        assertThat(result).isEqualTo(mockContext);
    }

    @Test
    void shouldCombineSingleAndBulkOperations() {
        List<String> moreTasks = Arrays.asList("task3", "task4");
        Map<String, Object> moreVars = Map.of("key3", "value3");

        WorkflowContext result = builder
            .task("task1")
            .task("task2")
            .tasks(moreTasks)
            .variable("key1", "value1")
            .variable("key2", 42)
            .variables(moreVars)
            .start();

        ArgumentCaptor<List<String>> taskCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> varCaptor = ArgumentCaptor.forClass(Map.class);

        verify(workflowEngine).startWorkflow(eq("case-123"), taskCaptor.capture(), varCaptor.capture());

        List<String> capturedTasks = taskCaptor.getValue();
        assertThat(capturedTasks).containsExactly("task1", "task2", "task3", "task4");

        Map<String, Object> capturedVars = varCaptor.getValue();
        assertThat(capturedVars)
            .containsEntry("key1", "value1")
            .containsEntry("key2", 42)
            .containsEntry("key3", "value3");
    }

    @Test
    void shouldHandleEmptyVariables() {
        WorkflowContext result = builder
            .task("task1")
            .start();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(workflowEngine).startWorkflow(eq("case-123"), anyList(), captor.capture());

        Map<String, Object> variables = captor.getValue();
        assertThat(variables).isEmpty();
    }

    @Test
    void shouldBuildEmptyVariables() {
        WorkflowVariables result = builder.buildVariables();

        assertThat(result).isNotNull();
        // WorkflowVariables doesn't have a direct way to check if empty,
        // but we can verify it was created
    }

    @Test
    void shouldMaintainCaseIdThroughoutChain() {
        // The caseId is used in the start() method
        builder
            .task("task1")
            .variable("key", "value")
            .start();

        verify(workflowEngine).startWorkflow(eq("case-123"), anyList(), anyMap());
    }
}
