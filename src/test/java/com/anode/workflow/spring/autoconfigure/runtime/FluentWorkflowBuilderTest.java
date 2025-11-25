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
        // Mock buildDefinitionFromNodes to return a valid definition
        WorkflowDefinition mockDefinition = new WorkflowDefinition();
        lenient().when(workflowEngine.buildDefinitionFromNodes(anyList())).thenReturn(mockDefinition);

        // Mock startWorkflow calls
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
        verify(workflowEngine).buildDefinitionFromNodes(anyList());
        verify(workflowEngine).startWorkflow(eq("case-123"), any(WorkflowDefinition.class), any(WorkflowVariables.class));
    }

    @Test
    void shouldBuildAndStartWorkflowWithMultipleTasks() {
        WorkflowContext result = builder
            .task("task1")
            .task("task2")
            .task("task3")
            .start();

        assertThat(result).isEqualTo(mockContext);
        verify(workflowEngine).buildDefinitionFromNodes(anyList());
        verify(workflowEngine).startWorkflow(eq("case-123"), any(WorkflowDefinition.class), any(WorkflowVariables.class));
    }

    @Test
    void shouldAddSingleVariable() {
        WorkflowContext result = builder
            .task("task1")
            .variable("key", "value")
            .start();

        ArgumentCaptor<WorkflowVariables> captor = ArgumentCaptor.forClass(WorkflowVariables.class);
        verify(workflowEngine).startWorkflow(eq("case-123"), any(WorkflowDefinition.class), captor.capture());

        WorkflowVariables variables = captor.getValue();
        assertThat(variables.getString("key")).isEqualTo("value");
    }

    @Test
    void shouldAddMultipleVariables() {
        WorkflowContext result = builder
            .task("task1")
            .variable("key1", "value1")
            .variable("key2", 42)
            .variable("key3", true)
            .start();

        ArgumentCaptor<WorkflowVariables> captor = ArgumentCaptor.forClass(WorkflowVariables.class);
        verify(workflowEngine).startWorkflow(eq("case-123"), any(WorkflowDefinition.class), captor.capture());

        WorkflowVariables variables = captor.getValue();
        assertThat(variables.getString("key1")).isEqualTo("value1");
        assertThat(variables.getInteger("key2")).isEqualTo(42);
        assertThat(variables.getBoolean("key3")).isEqualTo(true);
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

        ArgumentCaptor<WorkflowVariables> captor = ArgumentCaptor.forClass(WorkflowVariables.class);
        verify(workflowEngine).startWorkflow(eq("case-123"), any(WorkflowDefinition.class), captor.capture());

        WorkflowVariables variables = captor.getValue();
        assertThat(variables.getString("key1")).isEqualTo("value1");
        assertThat(variables.getString("key2")).isEqualTo("value2");
    }

    @Test
    void shouldSelectSpecificEngine() {
        WorkflowContext result = builder
            .engine("custom-engine")
            .task("task1")
            .start();

        assertThat(result).isEqualTo(mockContext);
        verify(workflowEngine).buildDefinitionFromNodes(anyList());
        verify(workflowEngine).startWorkflow(eq("case-123"), eq("custom-engine"),
            any(WorkflowDefinition.class), any(WorkflowVariables.class));
    }

    @Test
    void shouldBuildDefinitionWithoutStarting() {
        WorkflowDefinition mockDefinition = new WorkflowDefinition();
        when(workflowEngine.buildDefinitionFromNodes(anyList())).thenReturn(mockDefinition);

        WorkflowDefinition result = builder
            .task("task1")
            .task("task2")
            .buildDefinition();

        assertThat(result).isEqualTo(mockDefinition);
        verify(workflowEngine).buildDefinitionFromNodes(anyList());
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
        verify(workflowEngine).buildDefinitionFromNodes(anyList());
        verify(workflowEngine).startWorkflow(eq("case-123"), eq("custom-engine"),
            any(WorkflowDefinition.class), any(WorkflowVariables.class));
    }

    @Test
    void shouldCombineSingleAndBulkOperations() {
        List<String> moreTasks = Arrays.asList("task3", "task4");
        Map<String, Object> moreVars = Map.of("key3", "value3");

        WorkflowContext result = builder
            .task("task1")
            .task("task2")
            .task(moreTasks.get(0))
            .task(moreTasks.get(1))
            .variable("key1", "value1")
            .variable("key2", 42)
            .variables(moreVars)
            .start();

        ArgumentCaptor<WorkflowVariables> varCaptor = ArgumentCaptor.forClass(WorkflowVariables.class);

        verify(workflowEngine).buildDefinitionFromNodes(anyList());
        verify(workflowEngine).startWorkflow(eq("case-123"), any(WorkflowDefinition.class), varCaptor.capture());

        WorkflowVariables capturedVars = varCaptor.getValue();
        assertThat(capturedVars.getString("key1")).isEqualTo("value1");
        assertThat(capturedVars.getInteger("key2")).isEqualTo(42);
        assertThat(capturedVars.getString("key3")).isEqualTo("value3");
    }

    @Test
    void shouldHandleEmptyVariables() {
        WorkflowContext result = builder
            .task("task1")
            .start();

        ArgumentCaptor<WorkflowVariables> captor = ArgumentCaptor.forClass(WorkflowVariables.class);
        verify(workflowEngine).startWorkflow(eq("case-123"), any(WorkflowDefinition.class), captor.capture());

        WorkflowVariables variables = captor.getValue();
        assertThat(variables).isNotNull();
        // Empty WorkflowVariables is still valid
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

        verify(workflowEngine).buildDefinitionFromNodes(anyList());
        verify(workflowEngine).startWorkflow(eq("case-123"), any(WorkflowDefinition.class), any(WorkflowVariables.class));
    }
}
