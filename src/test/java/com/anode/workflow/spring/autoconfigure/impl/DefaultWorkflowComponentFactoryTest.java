package com.anode.workflow.spring.autoconfigure.impl;

import com.anode.workflow.entities.steps.InvokableRoute;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.entities.steps.Step.StepType;
import com.anode.workflow.entities.workflows.WorkflowContext;
import com.anode.workflow.service.WorkflowComponantFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.stream.Stream;

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
    private ObjectProvider<InvokableTask> mockTaskProvider;
    private ObjectProvider<InvokableRoute> mockRouteProvider;

    @BeforeEach
    void setUp() {
        mockContext = mock(ApplicationContext.class);
        mockTaskProvider = mock(ObjectProvider.class);
        mockRouteProvider = mock(ObjectProvider.class);

        // By default, return empty streams
        when(mockTaskProvider.stream()).thenReturn(Stream.empty());
        when(mockRouteProvider.stream()).thenReturn(Stream.empty());

        factory = new DefaultWorkflowComponentFactory(mockContext, mockTaskProvider, mockRouteProvider);
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
        // Use the actual class name of the mock as the component name
        when(context.getCompName()).thenReturn(mockTask.getClass().getName());

        // Re-initialize factory with mock task (use fresh providers to avoid stream reuse)
        ObjectProvider<InvokableTask> taskProvider = mock(ObjectProvider.class);
        ObjectProvider<InvokableRoute> emptyRouteProvider = mock(ObjectProvider.class);
        when(taskProvider.stream()).thenReturn(Stream.of(mockTask));
        when(emptyRouteProvider.stream()).thenReturn(Stream.empty());

        factory = new DefaultWorkflowComponentFactory(mockContext, taskProvider, emptyRouteProvider);
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
            .hasMessageContaining("Component type cannot be null");
    }

    @Test
    void shouldThrowExceptionForMissingTask() {
        // Given
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.TASK);
        when(context.getCompName()).thenReturn("nonExistentTask");

        // Re-initialize factory with empty providers (fresh providers to avoid stream reuse)
        ObjectProvider<InvokableTask> emptyTaskProvider = mock(ObjectProvider.class);
        ObjectProvider<InvokableRoute> emptyRouteProvider = mock(ObjectProvider.class);
        when(emptyTaskProvider.stream()).thenReturn(Stream.empty());
        when(emptyRouteProvider.stream()).thenReturn(Stream.empty());

        factory = new DefaultWorkflowComponentFactory(mockContext, emptyTaskProvider, emptyRouteProvider);
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

    @Test
    void shouldThrowExceptionForNullContext() {
        // When/Then
        assertThatThrownBy(() -> factory.getObject(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WorkflowContext cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullComponentType() {
        // Given
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(null);
        when(context.getCompName()).thenReturn("testTask");

        // When/Then
        assertThatThrownBy(() -> factory.getObject(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Component type cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullComponentName() {
        // Given
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.TASK);
        when(context.getCompName()).thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> factory.getObject(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Component name cannot be null");
    }

    @Test
    void shouldThrowExceptionForEmptyComponentName() {
        // Given
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.TASK);
        when(context.getCompName()).thenReturn("  ");

        // When/Then
        assertThatThrownBy(() -> factory.getObject(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Component name cannot be null or empty");
    }

    @Test
    void shouldReturnRouteForSRouteContext() {
        // Given
        InvokableRoute mockRoute = mock(InvokableRoute.class);

        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.S_ROUTE);
        when(context.getCompName()).thenReturn(mockRoute.getClass().getName());

        // Re-initialize factory with mock route
        ObjectProvider<InvokableTask> emptyTaskProvider = mock(ObjectProvider.class);
        ObjectProvider<InvokableRoute> routeProvider = mock(ObjectProvider.class);
        when(emptyTaskProvider.stream()).thenReturn(Stream.empty());
        when(routeProvider.stream()).thenReturn(Stream.of(mockRoute));

        factory = new DefaultWorkflowComponentFactory(mockContext, emptyTaskProvider, routeProvider);
        factory.init();

        // When
        Object result = factory.getObject(context);

        // Then
        assertThat(result).isEqualTo(mockRoute);
    }

    @Test
    void shouldReturnRouteForPRouteContext() {
        // Given
        InvokableRoute mockRoute = mock(InvokableRoute.class);

        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.P_ROUTE);
        when(context.getCompName()).thenReturn(mockRoute.getClass().getName());

        // Re-initialize factory with mock route
        ObjectProvider<InvokableTask> emptyTaskProvider = mock(ObjectProvider.class);
        ObjectProvider<InvokableRoute> routeProvider = mock(ObjectProvider.class);
        when(emptyTaskProvider.stream()).thenReturn(Stream.empty());
        when(routeProvider.stream()).thenReturn(Stream.of(mockRoute));

        factory = new DefaultWorkflowComponentFactory(mockContext, emptyTaskProvider, routeProvider);
        factory.init();

        // When
        Object result = factory.getObject(context);

        // Then
        assertThat(result).isEqualTo(mockRoute);
    }

    @Test
    void shouldReturnRouteForPRouteDynamicContext() {
        // Given
        InvokableRoute mockRoute = mock(InvokableRoute.class);

        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.P_ROUTE_DYNAMIC);
        when(context.getCompName()).thenReturn(mockRoute.getClass().getName());

        // Re-initialize factory with mock route
        ObjectProvider<InvokableTask> emptyTaskProvider = mock(ObjectProvider.class);
        ObjectProvider<InvokableRoute> routeProvider = mock(ObjectProvider.class);
        when(emptyTaskProvider.stream()).thenReturn(Stream.empty());
        when(routeProvider.stream()).thenReturn(Stream.of(mockRoute));

        factory = new DefaultWorkflowComponentFactory(mockContext, emptyTaskProvider, routeProvider);
        factory.init();

        // When
        Object result = factory.getObject(context);

        // Then
        assertThat(result).isEqualTo(mockRoute);
    }

    @Test
    void shouldThrowExceptionForPauseType() {
        // Given
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.PAUSE);
        when(context.getCompName()).thenReturn("testPause");

        // When/Then
        assertThatThrownBy(() -> factory.getObject(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported component type: PAUSE")
            .hasMessageContaining("should be handled by the Runtime Service");
    }

    @Test
    void shouldThrowExceptionForPJoinType() {
        // Given
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.P_JOIN);
        when(context.getCompName()).thenReturn("testJoin");

        // When/Then
        assertThatThrownBy(() -> factory.getObject(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported component type: P_JOIN")
            .hasMessageContaining("should be handled by the Runtime Service");
    }

    @Test
    void shouldThrowExceptionForPersistType() {
        // Given
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.PERSIST);
        when(context.getCompName()).thenReturn("testPersist");

        // When/Then
        assertThatThrownBy(() -> factory.getObject(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported component type: PERSIST")
            .hasMessageContaining("should be handled by the Runtime Service");
    }

    @Test
    void shouldThrowExceptionForMissingRoute() {
        // Given
        WorkflowContext context = mock(WorkflowContext.class);
        when(context.getCompType()).thenReturn(StepType.S_ROUTE);
        when(context.getCompName()).thenReturn("nonExistentRoute");

        // Re-initialize factory with empty providers (fresh providers to avoid stream reuse)
        ObjectProvider<InvokableTask> emptyTaskProvider = mock(ObjectProvider.class);
        ObjectProvider<InvokableRoute> emptyRouteProvider = mock(ObjectProvider.class);
        when(emptyTaskProvider.stream()).thenReturn(Stream.empty());
        when(emptyRouteProvider.stream()).thenReturn(Stream.empty());

        factory = new DefaultWorkflowComponentFactory(mockContext, emptyTaskProvider, emptyRouteProvider);
        factory.init();

        // When/Then
        assertThatThrownBy(() -> factory.getObject(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No InvokableRoute bean found");
    }
}
