package com.anode.workflow.spring.autoconfigure.impl;

import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.Step.StepType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.WorkflowComponantFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultWorkflowComponentFactory}.
 */
class DefaultWorkflowComponentFactoryTest {

    private DefaultWorkflowComponentFactory factory;
    private ApplicationContext mockContext;

    @BeforeEach
    void setUp() {
        mockContext = mock(ApplicationContext.class);
        factory = new DefaultWorkflowComponentFactory(mockContext);
        factory.init();
    }

    @Test
    void shouldImplementWorkflowComponentFactory() {
        assertThat(factory).isInstanceOf(WorkflowComponantFactory.class);
    }

    @Test
    void shouldReturnTaskForTaskContext() {
        // Given
        InvokableTask mockTask = mock(InvokableTask.class);
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.TASK);
        when(context.getCompName()).thenReturn("testTask");
        when(mockContext.getBeansOfType(InvokableTask.class))
            .thenReturn(java.util.Map.of("testTask", mockTask));

        // Re-initialize factory to pick up the mock beans
        factory = new DefaultWorkflowComponentFactory(mockContext);
        factory.init();

        // When
        Object result = factory.getObject(context);

        // Then
        assertThat(result).isEqualTo(mockTask);
    }

    @Test
    void shouldThrowExceptionForUnsupportedComponentType() {
        // Given
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> factory.getObject(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported component type");
    }

    @Test
    void shouldThrowExceptionForMissingTask() {
        // Given
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.TASK);
        when(context.getCompName()).thenReturn("nonExistentTask");
        when(mockContext.getBeansOfType(InvokableTask.class))
            .thenReturn(java.util.Map.of());

        // Re-initialize factory
        factory = new DefaultWorkflowComponentFactory(mockContext);
        factory.init();

        // When/Then
        assertThatThrownBy(() -> factory.getObject(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No InvokableTask bean found");
    }

    @Test
    void shouldBeInstantiable() {
        assertThat(factory).isNotNull();
    }
}
